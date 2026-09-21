package br.com.auralab.clientes.service;

import br.com.auralab.clientes.api.dto.*;
import br.com.auralab.clientes.domain.*;
import br.com.auralab.clientes.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de cartões e bandeiras: lista bandeiras, adiciona cartão, define o preferencial
 * e inativa. Por decisão do projeto não existe BandeiraService: bandeiras ficam aqui.
 *
 * <p>Requisitos: RF0027 (múltiplos cartões com um único preferencial ativo), RN0024
 * (número e CVV validados e descartados; persistem só os últimos quatro dígitos), RN0025
 * (bandeira cadastrada) e RNF0012 (auditoria na mesma transação).
 */
@Service
public class CartaoService {

  private final ClienteRepository clientes;
  private final BandeiraRepository bandeiras;
  private final AuditoriaService auditoria;
  private final ClienteMapper mapper;

  public CartaoService(
      ClienteRepository c, BandeiraRepository b, AuditoriaService a, ClienteMapper m) {
    clientes = c;
    bandeiras = b;
    auditoria = a;
    mapper = m;
  }

  @Transactional(readOnly = true)
  public List<BandeiraView> listarBandeiras() {
    return bandeiras.findAll().stream().map(b -> new BandeiraView(b.getId(), b.getNome())).toList();
  }

  @Transactional(readOnly = true)
  public List<CartaoView> listar(Long clienteId) {
    return cliente(clienteId).getCartoes().stream().map(mapper::cartao).toList();
  }

  /**
   * RN0024/RN0025: valida número (Luhn) e CVV sem persistir nenhum dos dois; o primeiro
   * cartão ativo do cliente passa a ser preferencial automaticamente (RF0027).
   */
  @Transactional
  public CartaoView adicionar(Long clienteId, CartaoInput in) {
    Cliente c = bloqueado(clienteId);
    Map<String, Object> antes = RegistroCliente.cartoes(c);
    String numero = digitos(in.numero()), cvv = in.codigoSeguranca();
    if (numero.length() < 13 || numero.length() > 19 || !luhn(numero))
      throw new RegraNegocioException(
          "CARTAO_INVALIDO", "Informe um número de cartão fictício válido.");
    if (!cvv.matches("[0-9]{3,4}"))
      throw new RegraNegocioException(
          "CODIGO_SEGURANCA_INVALIDO", "Código de segurança deve ter três ou quatro dígitos.");
    Bandeira b =
        bandeiras
            .findById(in.bandeiraId())
            .orElseThrow(
                () ->
                    new RegraNegocioException(
                        "BANDEIRA_INVALIDA", "Selecione uma bandeira cadastrada."));
    boolean preferencial = c.getCartoes().stream().noneMatch(x -> x.isAtivo()) || in.preferencial();
    if (preferencial) {
      c.getCartoes().forEach(x -> x.definirPreferencial(false));
      clientes.flush();
    }
    Cartao cartao =
        new Cartao(b, in.titular().trim(), numero.substring(numero.length() - 4), preferencial);
    c.adicionarCartao(cartao);
    clientes.flush();
    auditoria.registrar(
        clienteId, "ADICIONAR_CARTAO", "CARTAO", cartao.getId(), antes, RegistroCliente.cartoes(c));
    return mapper.cartao(cartao);
  }

  @Transactional
  public void preferencial(Long clienteId, Long id) {
    Cliente c = bloqueado(clienteId);
    Map<String, Object> antes = RegistroCliente.cartoes(c);
    Cartao alvo = cartao(c, id);
    if (!alvo.isAtivo())
      throw new RegraNegocioException(
          "CARTAO_INATIVO", "Cartão inativo não pode ser preferencial.");
    c.getCartoes().forEach(x -> x.definirPreferencial(false));
    clientes.flush();
    alvo.definirPreferencial(true);
    clientes.flush();
    auditoria.registrar(
        clienteId, "DEFINIR_CARTAO_PREFERENCIAL", "CARTAO", id, antes, RegistroCliente.cartoes(c));
  }

  /** Se o cartão inativado era o preferencial, outro cartão ativo assume (RF0027). */
  @Transactional
  public void inativar(Long clienteId, Long id) {
    Cliente c = bloqueado(clienteId);
    Map<String, Object> antes = RegistroCliente.cartoes(c);
    Cartao alvo = cartao(c, id);
    if (!alvo.isAtivo())
      throw new RegraNegocioException("CARTAO_INATIVO", "Cartão já está inativo.");
    boolean era = alvo.isPreferencial();
    alvo.inativar();
    clientes.flush();
    if (era)
      c.getCartoes().stream()
          .filter(Cartao::isAtivo)
          .findFirst()
          .ifPresent(x -> x.definirPreferencial(true));
    clientes.flush();
    auditoria.registrar(
        clienteId, "INATIVAR_CARTAO", "CARTAO", id, antes, RegistroCliente.cartoes(c));
  }

  private Cliente cliente(Long id) {
    return clientes
        .findById(id)
        .orElseThrow(
            () -> new RegraNegocioException("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado."));
  }

  private Cliente bloqueado(Long id) {
    return clientes
        .buscarParaAtualizacao(id)
        .orElseThrow(
            () -> new RegraNegocioException("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado."));
  }

  private Cartao cartao(Cliente c, Long id) {
    return c.getCartoes().stream()
        .filter(x -> x.getId().equals(id))
        .findFirst()
        .orElseThrow(
            () ->
                new RegraNegocioException(
                    "CARTAO_NAO_ENCONTRADO", "Cartão não encontrado para este cliente."));
  }

  private String digitos(String v) {
    if (v != null && !v.matches("[0-9 -]+"))
      throw new RegraNegocioException("CARTAO_INVALIDO", "Número fictício inválido.");
    return v == null ? "" : v.replaceAll("\\D", "");
  }

  // Verificação de Luhn para números de cartão fictícios em ambiente demonstrativo.
  private boolean luhn(String n) {
    int soma = 0;
    boolean dobro = false;
    for (int i = n.length() - 1; i >= 0; i--) {
      int d = n.charAt(i) - 48;
      if (dobro && (d *= 2) > 9) d -= 9;
      soma += d;
      dobro = !dobro;
    }
    return soma % 10 == 0;
  }
}
