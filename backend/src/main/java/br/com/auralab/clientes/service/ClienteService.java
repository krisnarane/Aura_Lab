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

  @Transactional
  public void alterarSenha(Long id, SenhaInput in) {
    Cliente c = repo.buscarParaAtualizacao(id).orElseThrow(() -> naoEncontrado());
    validador.senha(in.novaSenha(), in.confirmacaoSenha());
    c.alterarSenha(encoder.encode(in.novaSenha()));
    auditoria.registrar(id, "ALTERAR_SENHA", "CLIENTE", id, null, Map.of("senhaAlterada", true));
  }

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
