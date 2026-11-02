package br.com.auralab.ia.service;

import br.com.auralab.clientes.service.AuditoriaService;
import br.com.auralab.clientes.service.RegraNegocioException;
import br.com.auralab.ia.repository.IaRepository;
import br.com.auralab.ia.repository.IaRepository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** RNF0044: historico e preferencias reais; a IA apenas redige e escolhe IDs permitidos. */
@Service
public class IaService {
  public record Resposta(Long conversaId, Mensagem mensagem, List<Produto> produtos) {}

  private final IaRepository repo;
  private final GeradorIa gerador;
  private final ObjectMapper json;
  private final AuditoriaService auditoria;
  private final TransactionTemplate transacao;
  private final int maxPergunta;

  public IaService(IaRepository repo, GeradorIa gerador, ObjectMapper json,
      AuditoriaService auditoria, TransactionTemplate transacao,
      @Value("${aura.ia.max-question-chars:1000}") int maxPergunta) {
    this.repo = repo;
    this.gerador = gerador;
    this.json = json;
    this.auditoria = auditoria;
    this.transacao = transacao;
    this.maxPergunta = maxPergunta;
  }

  public void exigirCliente(Long cliente) {
    if (!repo.clienteExiste(cliente))
      throw new RegraNegocioException("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado.");
    if (!repo.clienteAtivo(cliente))
      throw new RegraNegocioException("CLIENTE_INATIVO", "Cliente inativo.");
  }

  public List<String> preferencias(Long cliente) {
    exigirCliente(cliente);
    return repo.preferencias(cliente);
  }

  @Transactional
  public List<String> salvarPreferencias(Long cliente, List<String> categorias) {
    exigirCliente(cliente);
    if (categorias == null || categorias.size() < 1 || categorias.size() > 5
        || categorias.stream().anyMatch(Objects::isNull)
        || new HashSet<>(categorias).size() != categorias.size()
        || !repo.categoriasValidas(categorias))
      throw new RegraNegocioException("PREFERENCIAS_INVALIDAS", "Selecione de uma a cinco categorias cadastradas, sem repetição.");
    List<String> antes = repo.preferencias(cliente);
    repo.substituirPreferencias(cliente, categorias);
    auditoria.registrar(cliente, "ALTERAR_PREFERENCIAS", "CLIENTE", cliente,
        Map.of("categorias", antes), Map.of("categorias", categorias));
    return repo.preferencias(cliente);
  }

  public List<Conversa> conversas(Long cliente) {
    exigirCliente(cliente);
    return repo.conversas(cliente);
  }

  public List<Mensagem> mensagens(Long cliente, Long conversa) {
    exigirCliente(cliente);
    if (!repo.conversaDoCliente(conversa, cliente))
      throw new RegraNegocioException("IA_CONVERSA_NAO_ENCONTRADA", "Conversa não encontrada.");
    return repo.mensagens(conversa);
  }

  public Resposta enviar(Long cliente, Long conversa, String pergunta) {
    exigirCliente(cliente);
    if (pergunta == null || pergunta.isBlank() || pergunta.length() > maxPergunta)
      throw new RegraNegocioException("IA_PERGUNTA_INVALIDA", "Pergunta fora do limite configurado.");
    if (conversa != null && !repo.conversaDoCliente(conversa, cliente))
      throw new RegraNegocioException("IA_CONVERSA_NAO_ENCONTRADA", "Conversa não encontrada.");
    Modelo modelo = repo.modeloAtivo().orElseThrow(IaIndisponivelException::new);
    List<Produto> candidatos = repo.candidatos(cliente);
    List<Mensagem> historico = conversa == null ? List.of() : repo.mensagens(conversa);
    String prompt = criarPrompt(pergunta.trim(), repo.preferencias(cliente), repo.compras(cliente), candidatos, historico);
    String bruto = gerador.gerar(modelo.endpoint(), prompt);
    String texto;
    List<Long> ids;
    try {
      String limpo = bruto.trim().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
      JsonNode resposta = json.readTree(limpo);
      texto = resposta.path("texto").asText().trim();
      if (texto.isBlank() || texto.length() > 6000) throw new IllegalArgumentException();
      Set<Long> permitidos = new HashSet<>();
      for (Produto p : candidatos) permitidos.add(p.id());
      ids = new ArrayList<>();
      if (resposta.path("produtosIds").isArray())
        for (JsonNode n : resposta.path("produtosIds"))
          if (n.canConvertToLong() && permitidos.contains(n.asLong()) && !ids.contains(n.asLong()))
            ids.add(n.asLong());
    } catch (Exception ex) {
      throw new IaIndisponivelException();
    }
    List<Long> recomendados = List.copyOf(ids);
    List<Produto> produtos = candidatos.stream().filter(p -> recomendados.contains(p.id())).toList();
    return transacao.execute(status -> {
      exigirCliente(cliente);
      Long conversaId = conversa == null ? repo.criarConversa(cliente) : conversa;
      if (conversa != null && !repo.conversaDoCliente(conversa, cliente))
        throw new RegraNegocioException("IA_CONVERSA_NAO_ENCONTRADA", "Conversa não encontrada.");
      repo.criarMensagem(conversaId, "USER", pergunta.trim(), List.of(), null);
      Long respostaId = repo.criarMensagem(conversaId, "ASSISTANT", texto, recomendados, modelo.versao());
      auditoria.registrar(cliente, "RESPONDER_CHAT", "IA_CONVERSA", conversaId,
          null, Map.of("respostaId", respostaId, "modeloVersao", modelo.versao()));
      return new Resposta(conversaId, repo.resposta(respostaId).orElseThrow(), produtos);
    });
  }

  private String criarPrompt(String pergunta, List<String> preferencias, List<String> compras,
      List<Produto> candidatos, List<Mensagem> historico) {
    try {
      List<Map<String, String>> anteriores = historico.stream()
          .skip(Math.max(0, historico.size() - 6))
          .map(m -> Map.of("papel", m.papel(), "texto", m.texto())).toList();
      return "Você é a consultora do Aura Lab. Responda em português, ajude a buscar produtos, "
          + "esclareça dúvidas e sugira conteúdo relevante. Use somente os fatos do catálogo fornecido; "
          + "não invente produtos, preços, estoque nem efeitos clínicos. "
          + "Retorne apenas JSON {\"texto\":\"resposta\",\"produtosIds\":[ids]}. "
          + "Para dúvidas sem produto adequado, responda sem IDs. Dados: "
          + json.writeValueAsString(Map.of("preferencias", preferencias, "comprasAprovadas", compras,
              "catalogo", candidatos, "conversaRecente", anteriores, "pergunta", pergunta));
    } catch (Exception ex) { throw new IllegalStateException(ex); }
  }

  @Transactional
  public Mensagem avaliar(Long cliente, Long resposta, Boolean util, String motivo) {
    exigirCliente(cliente);
    if (!repo.respostaDoCliente(resposta, cliente))
      throw new RegraNegocioException("IA_RESPOSTA_NAO_ENCONTRADA", "Resposta não encontrada.");
    if (util == null || (motivo != null && motivo.length() > 300))
      throw new RegraNegocioException("IA_AVALIACAO_INVALIDA", "Avaliação inválida.");
    Mensagem antes = repo.resposta(resposta).orElseThrow();
    repo.avaliar(resposta, util, util ? null : (motivo == null ? null : motivo.trim()));
    auditoria.registrar(cliente, "AVALIAR_CHAT", "IA_MENSAGEM", resposta,
        Map.of("util", antes.util() == null ? "SEM_AVALIACAO" : antes.util()), Map.of("util", util));
    return repo.resposta(resposta).orElseThrow();
  }
}
