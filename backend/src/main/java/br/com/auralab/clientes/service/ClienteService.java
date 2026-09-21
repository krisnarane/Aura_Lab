package br.com.auralab.clientes.service;

import br.com.auralab.clientes.api.dto.*;
import br.com.auralab.clientes.domain.*;
import br.com.auralab.clientes.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service do cadastro de clientes: cadastrar, consultar, alterar, inativar e alterar
 * senha. Escrita e auditoria acontecem na mesma transação (RNF0012).
 *
 * <p>Requisitos: RF0021 (cadastro com endereço inicial), RF0022 (alteração de dados),
 * RF0023 (inativação lógica), RF0024 (consulta com filtros combináveis), RF0028
 * (alteração apenas de senha), RN0026 (dados obrigatórios e unicidade), RN0027 (ranking
 * via Strategy), RNF0031 a RNF0033 (senha forte, confirmada e com BCrypt) e RNF0035
 * (código único). Por decisão do projeto não existe SenhaService: a senha fica aqui.
 */
@Service
public class ClienteService {

  private final ClienteRepository repo;
  private final ValidadorCliente validador;
  private final PasswordEncoder encoder;
  private final AuditoriaService auditoria;
  private final ClienteMapper mapper;
  private final TransacaoRepository transacoes;
  private final PoliticaRankingCliente ranking;
  @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

  public ClienteService(
      ClienteRepository r,
      ValidadorCliente v,
      PasswordEncoder e,
      AuditoriaService a,
      ClienteMapper m,
      TransacaoRepository t,
      PoliticaRankingCliente p) {
    transacoes = t;
    ranking = p;
    repo = r;
    validador = v;
    encoder = e;
    auditoria = a;
    mapper = m;
  }

  /** RF0028 + RNF0031/RNF0032/RNF0033: valida força e confirmação, grava só o hash BCrypt. */
  @Transactional
  public void alterarSenha(Long id, SenhaInput in) {
    Cliente c = repo.buscarParaAtualizacao(id).orElseThrow(() -> naoEncontrado());
    validador.senha(in.novaSenha(), in.confirmacaoSenha());
    c.alterarSenha(encoder.encode(in.novaSenha()));
    auditoria.registrar(id, "ALTERAR_SENHA", "CLIENTE", id, null, Map.of("senhaAlterada", true));
  }

  /**
   * RF0021 + RN0026 + RNF0035: cria o cliente já com o endereço residencial, que cumpre as
   * finalidades de cobrança e entrega (RN0021/RN0022). O código CLI- vem do trigger do
   * banco e é lido via refresh após o flush.
   */
  @Transactional
  public ClienteView cadastrar(ClienteInput in) {
    validador.senha(in.senha(), in.confirmacaoSenha());
    validador.nascimento(in.nascimento());
    String cpf = validador.cpf(in.cpf()), email = in.email().trim().toLowerCase(Locale.ROOT);
    validarUnicos(cpf, email, null);
    Cliente c =
        new Cliente(
            in.nome().trim(),
            cpf,
            email,
            in.genero().trim(),
            in.nascimento(),
            in.telefoneTipo().trim(),
            validador.ddd(in.telefoneDdd()),
            validador.telefone(in.telefoneNumero()),
            encoder.encode(in.senha()));
    EnderecoInput x = in.enderecoResidencial();
    c.adicionarEndereco(novoEndereco(x, true, true, true));
    repo.saveAndFlush(c);
    em.refresh(c);
    Map<String, Object> dados = RegistroCliente.pessoal(c);
    dados.putAll(RegistroCliente.enderecos(c));
    auditoria.registrar(c.getId(), "CADASTRAR_CLIENTE", "CLIENTE", c.getId(), null, dados);
    return view(c);
  }

  /** RF0024: filtros isolados ou combinados com E; nome/e-mail parciais, demais exatos. */
  @Transactional(readOnly = true)
  public List<ClienteView> listar(
      String codigo,
      String nome,
      String cpf,
      String email,
      String genero,
      LocalDate nascimento,
      String tipo,
      String ddd,
      String numero,
      Boolean ativo) {
    return views(
        repo.findAll(
            ClienteSpecifications.filtrar(
                codigo, nome, cpf, email, genero, nascimento, tipo, ddd, numero, ativo),
            Sort.by("nome").and(Sort.by("id"))));
  }

  @Transactional(readOnly = true)
  public ClienteView buscar(Long id) {
    return view(entidade(id));
  }

  @Transactional
  public ClienteView alterar(Long id, ClienteAlteracaoInput in) {
    Cliente c = repo.buscarParaAtualizacao(id).orElseThrow(() -> naoEncontrado());
    validador.nascimento(in.nascimento());
    String cpf = validador.cpf(in.cpf()), email = in.email().trim().toLowerCase(Locale.ROOT);
    validarUnicos(cpf, email, id);
    Map<String, Object> antes = RegistroCliente.pessoal(c);
    c.alterar(
        in.nome().trim(),
        cpf,
        email,
        in.genero().trim(),
        in.nascimento(),
        in.telefoneTipo().trim(),
        validador.ddd(in.telefoneDdd()),
        validador.telefone(in.telefoneNumero()));
    auditoria.registrar(id, "ALTERAR_CLIENTE", "CLIENTE", id, antes, RegistroCliente.pessoal(c));
    return view(c);
  }

  /** RF0023: inativação lógica idempotente-negada (repetir retorna 409), com auditoria. */
  @Transactional
  public ClienteView inativar(Long id) {
    Cliente c = repo.buscarParaAtualizacao(id).orElseThrow(() -> naoEncontrado());
    if (!c.isAtivo())
      throw new RegraNegocioException("CLIENTE_INATIVO", "Este cliente já está inativo.");
    c.inativar();
    auditoria.registrar(
        id, "INATIVAR_CLIENTE", "CLIENTE", id, Map.of("ativo", true), Map.of("ativo", false));
    return view(c);
  }

  public Cliente entidade(Long id) {
    return repo.findById(id).orElseThrow(() -> naoEncontrado());
  }

  // Checagem prévia de duplicidade (RN0026); a restrição única do banco é a última linha.
  private void validarUnicos(String cpf, String email, Long id) {
    boolean dup =
        id == null
            ? (repo.existsByCpf(cpf) || repo.existsByEmailIgnoreCase(email))
            : (repo.existsByCpfAndIdNot(cpf, id)
                || repo.existsByEmailIgnoreCaseAndIdNot(email, id));
    if (dup)
      throw new RegraNegocioException(
          "CLIENTE_DUPLICADO", "Já existe cliente com este CPF ou e-mail.");
  }

  private Endereco novoEndereco(
      EnderecoInput x, boolean cobranca, boolean entrega, boolean preferencial) {
    return new Endereco(
        x.apelido().trim(),
        x.tipoResidencia().trim(),
        x.tipoLogradouro().trim(),
        x.logradouro().trim(),
        x.numero().trim(),
        x.bairro().trim(),
        validador.cep(x.cep()),
        x.cidade().trim(),
        x.estado().trim().toUpperCase(Locale.ROOT),
        x.pais().trim(),
        x.observacoes(),
        x.complemento(),
        cobranca,
        entrega,
        preferencial);
  }

  private ClienteView view(Cliente c) {
    return views(List.of(c)).getFirst();
  }

  /**
   * Monta as views em lote (endereços/cartões carregados por id e ranking RN0027 calculado
   * sobre os totais de compra) para atender RNF0011 evitando N+1 na consulta.
   */
  private List<ClienteView> views(List<Cliente> cs) {
    if (cs.isEmpty()) return List.of();
    var ids = cs.stream().map(Cliente::getId).toList();
    repo.carregarEnderecos(ids);
    repo.carregarCartoes(ids);
    Map<Long, Integer> scores = new HashMap<>();
    transacoes
        .totaisCompras(cs.stream().map(Cliente::getId).toList())
        .forEach(t -> scores.put(t.getClienteId(), ranking.calcular(t.getTotal())));
    return cs.stream().map(c -> mapper.view(c, scores.getOrDefault(c.getId(), 1))).toList();
  }

  private RegraNegocioException naoEncontrado() {
    return new RegraNegocioException("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado.");
  }
}
