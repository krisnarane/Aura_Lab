package br.com.auralab.clientes.service;

import br.com.auralab.clientes.api.dto.*;
import br.com.auralab.clientes.domain.*;
import br.com.auralab.clientes.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de endereços: listar, adicionar, alterar e inativar, mantendo os mínimos de
 * cobrança/entrega e a unicidade do preferencial de entrega.
 *
 * <p>Requisitos: RF0026 (múltiplos endereços com manutenção independente), RN0021/RN0022
 * (último endereço ativo de cada finalidade é protegido), RN0023 (validações de campos),
 * RNF0034 (endereços alterados sem tocar no restante do cadastro) e RNF0012 (auditoria na
 * mesma transação).
 */
@Service
public class EnderecoService {

  private final ClienteRepository repo;
  private final ValidadorCliente validador;
  private final AuditoriaService auditoria;
  private final ClienteMapper mapper;

  public EnderecoService(
      ClienteRepository r, ValidadorCliente v, AuditoriaService a, ClienteMapper m) {
    repo = r;
    validador = v;
    auditoria = a;
    mapper = m;
  }

  @Transactional(readOnly = true)
  public List<EnderecoView> listar(Long clienteId) {
    return cliente(clienteId).getEnderecos().stream().map(mapper::endereco).toList();
  }

  @Transactional
  public EnderecoView adicionar(Long clienteId, EnderecoInput in) {
    Cliente c = bloqueado(clienteId);
    Map<String, Object> antes = RegistroCliente.enderecos(c);
    validarFinalidade(in);
    if (in.preferencialEntrega()) c.getEnderecos().forEach(e -> e.definirPreferencial(false));
    Endereco e = novo(in);
    c.adicionarEndereco(e);
    normalizarPreferencia(c);
    repo.flush();
    auditoria.registrar(
        clienteId,
        "ADICIONAR_ENDERECO",
        "ENDERECO",
        e.getId(),
        antes,
        RegistroCliente.enderecos(c));
    return mapper.endereco(e);
  }

  @Transactional
  public EnderecoView alterar(Long clienteId, Long id, EnderecoInput in) {
    Cliente c = bloqueado(clienteId);
    Map<String, Object> antes = RegistroCliente.enderecos(c);
    validarFinalidade(in);
    Endereco e = endereco(c, id);
    if (!e.isAtivo())
      throw new RegraNegocioException("ENDERECO_INATIVO", "Endereço inativo não pode ser editado.");
    protegerMinimos(c, e, in.cobranca(), in.entrega());
    if (in.preferencialEntrega()) c.getEnderecos().forEach(x -> x.definirPreferencial(false));
    e.alterar(
        in.apelido().trim(),
        in.tipoResidencia().trim(),
        in.tipoLogradouro().trim(),
        in.logradouro().trim(),
        in.numero().trim(),
        in.bairro().trim(),
        validador.cep(in.cep()),
        in.cidade().trim(),
        in.estado().trim().toUpperCase(),
        in.pais().trim(),
        in.observacoes(),
        in.complemento(),
        in.cobranca(),
        in.entrega(),
        in.preferencialEntrega());
    normalizarPreferencia(c);
    repo.flush();
    auditoria.registrar(
        clienteId, "ALTERAR_ENDERECO", "ENDERECO", id, antes, RegistroCliente.enderecos(c));
    return mapper.endereco(e);
  }

  /**
   * Inativação lógica protegida por RN0021/RN0022: se o endereço inativado era o
   * preferencial de entrega, outro endereço ativo de entrega assume a preferência.
   */
  @Transactional
  public void inativar(Long clienteId, Long id) {
    Cliente c = bloqueado(clienteId);
    Map<String, Object> antes = RegistroCliente.enderecos(c);
    Endereco e = endereco(c, id);
    if (!e.isAtivo())
      throw new RegraNegocioException("ENDERECO_INATIVO", "Endereço já está inativo.");
    protegerMinimos(c, e, false, false);
    e.inativar();
    if (c.getEnderecos().stream()
        .noneMatch(x -> x.isAtivo() && x.isEntrega() && x.isPreferencialEntrega()))
      c.getEnderecos().stream()
          .filter(x -> x.isAtivo() && x.isEntrega())
          .findFirst()
          .ifPresent(x -> x.definirPreferencial(true));
    normalizarPreferencia(c);
    repo.flush();
    auditoria.registrar(
        clienteId, "INATIVAR_ENDERECO", "ENDERECO", id, antes, RegistroCliente.enderecos(c));
  }

  // Garante exatamente um preferencial entre os ativos de entrega (RF0026).
  private void normalizarPreferencia(Cliente c) {
    Endereco escolhido =
        c.getEnderecos().stream()
            .filter(e -> e.isAtivo() && e.isEntrega() && e.isPreferencialEntrega())
            .findFirst()
            .orElseGet(
                () ->
                    c.getEnderecos().stream()
                        .filter(e -> e.isAtivo() && e.isEntrega())
                        .findFirst()
                        .orElse(null));
    c.getEnderecos().forEach(e -> e.definirPreferencial(e == escolhido));
  }

  private Cliente cliente(Long id) {
    return repo.findById(id)
        .orElseThrow(
            () -> new RegraNegocioException("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado."));
  }

  // Bloqueia a linha do cliente (PESSIMISTIC_WRITE) para serializar escritas concorrentes.
  private Cliente bloqueado(Long id) {
    return repo.buscarParaAtualizacao(id)
        .orElseThrow(
            () -> new RegraNegocioException("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado."));
  }

  private Endereco endereco(Cliente c, Long id) {
    return c.getEnderecos().stream()
        .filter(e -> e.getId().equals(id))
        .findFirst()
        .orElseThrow(
            () ->
                new RegraNegocioException(
                    "ENDERECO_NAO_ENCONTRADO", "Endereço não encontrado para este cliente."));
  }

  private void validarFinalidade(EnderecoInput i) {
    if (!i.cobranca() && !i.entrega())
      throw new RegraNegocioException(
          "FINALIDADE_OBRIGATORIA", "Selecione endereço de cobrança e/ou entrega.");
    if (i.preferencialEntrega() && !i.entrega())
      throw new RegraNegocioException(
          "PREFERENCIAL_INVALIDO", "O endereço preferencial deve ser de entrega.");
    validador.cep(i.cep());
  }

  // RN0021/RN0022: impede remover a última finalidade de cobrança ou de entrega ativa.
  private void protegerMinimos(
      Cliente c, Endereco atual, boolean novaCobranca, boolean novaEntrega) {
    if (atual.isCobranca()
        && !novaCobranca
        && c.getEnderecos().stream().noneMatch(e -> e != atual && e.isAtivo() && e.isCobranca()))
      throw new RegraNegocioException(
          "ULTIMO_ENDERECO_COBRANCA", "O cliente deve manter ao menos um endereço de cobrança.");
    if (atual.isEntrega()
        && !novaEntrega
        && c.getEnderecos().stream().noneMatch(e -> e != atual && e.isAtivo() && e.isEntrega()))
      throw new RegraNegocioException(
          "ULTIMO_ENDERECO_ENTREGA", "O cliente deve manter ao menos um endereço de entrega.");
  }

  private Endereco novo(EnderecoInput i) {
    return new Endereco(
        i.apelido().trim(),
        i.tipoResidencia().trim(),
        i.tipoLogradouro().trim(),
        i.logradouro().trim(),
        i.numero().trim(),
        i.bairro().trim(),
        validador.cep(i.cep()),
        i.cidade().trim(),
        i.estado().trim().toUpperCase(),
        i.pais().trim(),
        i.observacoes(),
        i.complemento(),
        i.cobranca(),
        i.entrega(),
        i.preferencialEntrega());
  }
}
