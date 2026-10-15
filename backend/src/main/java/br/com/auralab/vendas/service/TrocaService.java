package br.com.auralab.vendas.service;

import static br.com.auralab.vendas.repository.CicloVendaRepository.*;

import br.com.auralab.clientes.repository.ClienteRepository;
import br.com.auralab.clientes.service.*;
import br.com.auralab.vendas.api.dto.*;
import br.com.auralab.vendas.domain.*;
import br.com.auralab.vendas.repository.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF0041--45/RF0054: troca parcial ou integral e recebimento atômico. */
@Service
public class TrocaService {
  private final CicloVendaRepository repo;
  private final CicloPedidoService ciclo;
  private final PedidoRepository pedidos;
  private final CupomRepository cupons;
  private final ClienteRepository clientes;
  private final ReservaEstoqueService reservas;
  private final AuditoriaService auditoria;
  private final com.fasterxml.jackson.databind.ObjectMapper json;
  @PersistenceContext private EntityManager em;

  public TrocaService(
      CicloVendaRepository repo,
      CicloPedidoService ciclo,
      PedidoRepository pedidos,
      CupomRepository cupons,
      ClienteRepository clientes,
      ReservaEstoqueService reservas,
      AuditoriaService auditoria,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    this.repo = repo;
    this.ciclo = ciclo;
    this.pedidos = pedidos;
    this.cupons = cupons;
    this.clientes = clientes;
    this.reservas = reservas;
    this.auditoria = auditoria;
    this.json = json;
  }

  @Transactional
  public TrocaView solicitar(Long cliente, Long pedido, SolicitacaoTrocaInput in, String chave) {
    repo.bloquearFluxo();
    var c =
        clientes
            .findById(cliente)
            .orElseThrow(
                () ->
                    new RegraNegocioException("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado."));
    if (!c.isAtivo())
      throw new RegraNegocioException(
          "CLIENTE_INATIVO", "Cliente inativo não pode solicitar troca.");
    String resumo = impressao(Map.of("pedidoId", pedido, "solicitacao", in));
    if (chave != null) {
      if (chave.isBlank() || chave.length() > 80)
        throw new RegraNegocioException("IDEMPOTENCIA_INVALIDA", "Chave de troca inválida.");
      Long anterior = repo.trocaExistente(cliente, chave, resumo);
      if (anterior != null) return buscar(cliente, anterior);
    }
    var p = ciclo.exigir(pedido);
    ciclo.exigirAtual(p);
    if (!p.getClienteId().equals(cliente))
      throw new RegraNegocioException(
          "PEDIDO_NAO_ENCONTRADO", "Pedido não encontrado para este cliente.");
    RegrasCicloVenda.exigirEstado(p.getStatus(), "ENTREGUE");
    var itens = p.getItens();
    var descontos =
        RegrasCicloVenda.ratearDesconto(
            itens.stream().map(ItemPedido::getSubtotal).toList(), p.getDesconto());
    Map<ItemPedido, Integer> selecionados = new LinkedHashMap<>();
    if (in.integral())
      for (var i : itens) {
        if (repo.comprometidas(i.getId()) > 0)
          throw new RegraNegocioException(
              "QUANTIDADE_TROCA_INVALIDA",
              "Compra já possui itens comprometidos em troca; solicite os itens restantes individualmente.");
        selecionados.put(i, i.getQuantidade());
      }
    else {
      if (in.itemPedidoId() == null || in.quantidade() == null || in.quantidade() < 1)
        throw new RegraNegocioException(
            "QUANTIDADE_TROCA_INVALIDA", "Selecione item e quantidade positiva.");
      var item =
          itens.stream()
              .filter(i -> i.getId().equals(in.itemPedidoId()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new RegraNegocioException(
                          "ITEM_PEDIDO_NAO_ENCONTRADO", "Item não pertence ao pedido."));
      if (in.quantidade() + repo.comprometidas(item.getId()) > item.getQuantidade())
        throw new RegraNegocioException(
            "QUANTIDADE_TROCA_INVALIDA", "Quantidade supera as unidades elegíveis para troca.");
      selecionados.put(item, in.quantidade());
    }
    Long id =
        repo.criarTroca(
            pedido, cliente, in.integral(), in.motivo().trim(), reservas.agora(), chave, resumo);
    for (var entrada : selecionados.entrySet()) {
      var item = entrada.getKey();
      int pos = itens.indexOf(item);
      BigDecimal credito =
          RegrasCicloVenda.creditoParcial(
              item.getSubtotal(),
              descontos.get(pos),
              item.getQuantidade(),
              repo.comprometidas(item.getId()),
              entrada.getValue());
      repo.adicionarItemTroca(id, item.getId(), entrada.getValue(), credito);
    }
    if (in.integral()) {
      var antes = RegistroVenda.pedido(p);
      p.transicionar("ENTREGUE", "EM_TROCA");
      pedidos.flush();
      auditoria.registrarComo(
          cliente,
          "CLIENTE_DEMO:" + cliente,
          reservas.agora(),
          "SOLICITAR_TROCA_INTEGRAL",
          "PEDIDO",
          pedido,
          antes,
          RegistroVenda.pedido(p));
      ciclo.registrarPedido(p);
    }
    var view = buscar(cliente, id);
    auditoria.registrarComo(
        cliente,
        "CLIENTE_DEMO:" + cliente,
        reservas.agora(),
        "SOLICITAR_TROCA",
        "TROCA",
        id,
        null,
        snapshot(view));
    registrarTransacao(view);
    return view;
  }

  @Transactional(readOnly = true)
  public List<TrocaView> listar(Long cliente, Long pedido) {
    return repo.trocas(cliente, pedido).stream().map(this::view).toList();
  }

  @Transactional(readOnly = true)
  public TrocaView buscar(Long cliente, Long id) {
    var r = repo.troca(id);
    if (cliente != null && !cliente.equals(id(r, "cliente_id")))
      throw new RegraNegocioException(
          "TROCA_NAO_ENCONTRADA", "Troca não encontrada para este cliente.");
    return view(r);
  }

  @Transactional
  public TrocaView autorizar(Long id) {
    repo.bloquearFluxo();
    var antes = buscar(null, id);
    if ("TROCA_AUTORIZADA".equals(antes.status())) return antes;
    RegrasCicloVenda.exigirEstado(antes.status(), "EM_TROCA");
    repo.autorizar(id, reservas.agora());
    if (antes.integral())
      mudarCompra(antes, "EM_TROCA", "TROCA_AUTORIZADA", "AUTORIZAR_TROCA_INTEGRAL");
    reservas.notificar(
        antes.clienteId(),
        "troca-autorizada-" + id,
        "TROCA_AUTORIZADA",
        "A troca "
            + antes.codigo()
            + " foi autorizada. Aguarde a confirmação de recebimento dos itens.");
    var depois = buscar(null, id);
    auditoria.registrarComo(
        antes.clienteId(),
        "ADMIN_DEMO",
        reservas.agora(),
        "AUTORIZAR_TROCA",
        "TROCA",
        id,
        snapshot(antes),
        snapshot(depois));
    registrarTransacao(depois);
    return depois;
  }

  @Transactional
  public TrocaView receber(Long id, boolean retornar) {
    repo.bloquearFluxo();
    var antes = buscar(null, id);
    if ("TROCADO".equals(antes.status())) {
      if (!Objects.equals(antes.retornarEstoque(), retornar))
        throw new RegraNegocioException(
            "IDEMPOTENCIA_CONFLITO", "Recebimento já concluído com outra decisão de estoque.");
      return antes;
    }
    RegrasCicloVenda.exigirEstado(antes.status(), "TROCA_AUTORIZADA");
    BigDecimal valor =
        antes.itens().stream()
            .map(TrocaView.Item::valorLiquido)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    var cupom = Cupom.troco(antes.clienteId(), valor, "Troca recebida " + antes.codigo());
    cupom.definirInstante(reservas.agora());
    cupons.saveAndFlush(cupom);
    em.refresh(cupom);
    if (retornar)
      for (var item : repo.itensTroca(id)) {
        repo.reentrar(
            id(item, "produto_id"), qtd(item, "quantidade"), id(item, "id"), reservas.agora());
        auditoria.registrarComo(
            antes.clienteId(),
            "ADMIN_DEMO",
            reservas.agora(),
            "REENTRAR_ESTOQUE",
            "ITEM_TROCA",
            id(item, "id"),
            Map.of("retornarEstoque", false),
            Map.of("produtoId", id(item, "produto_id"), "quantidade", qtd(item, "quantidade")));
      }
    repo.receber(id, retornar, cupom.getId(), reservas.agora());
    ciclo.registrarCupom(antes.clienteId(), cupom);
    if (antes.integral())
      mudarCompra(antes, "TROCA_AUTORIZADA", "TROCADO", "RECEBER_TROCA_INTEGRAL");
    var depois = buscar(null, id);
    auditoria.registrarComo(
        antes.clienteId(),
        "ADMIN_DEMO",
        reservas.agora(),
        "RECEBER_TROCA",
        "TROCA",
        id,
        snapshot(antes),
        snapshot(depois));
    registrarTransacao(depois);
    return depois;
  }

  private void mudarCompra(TrocaView troca, String de, String para, String operacao) {
    var p = ciclo.exigir(troca.pedidoId());
    var antes = RegistroVenda.pedido(p);
    p.transicionar(de, para);
    pedidos.flush();
    auditoria.registrarComo(
        p.getClienteId(),
        "ADMIN_DEMO",
        reservas.agora(),
        operacao,
        "PEDIDO",
        p.getId(),
        antes,
        RegistroVenda.pedido(p));
    ciclo.registrarPedido(p);
  }

  private TrocaView view(Map<String, Object> r) {
    Long id = id(r, "id"), cupomId = id(r, "cupom_id");
    var cupom = cupomId == null ? null : cupons.findById(cupomId).orElseThrow();
    String status = (String) r.get("status");
    var itens =
        repo.itensTroca(id).stream()
            .map(
                i ->
                    new TrocaView.Item(
                        id(i, "item_pedido_id"),
                        id(i, "produto_id"),
                        (String) i.get("produto_nome"),
                        qtd(i, "quantidade"),
                        (BigDecimal) i.get("valor_liquido")))
            .toList();
    return new TrocaView(
        id,
        (String) r.get("codigo"),
        id(r, "pedido_id"),
        id(r, "cliente_id"),
        (Boolean) r.get("integral"),
        status,
        (String) r.get("motivo"),
        tempo(r, "criada_em"),
        (Boolean) r.get("retornar_estoque"),
        cupom == null
            ? null
            : new CupomView(
                cupom.getId(),
                cupom.getCodigo(),
                cupom.getTipo(),
                cupom.getValor(),
                cupom.getPercentual(),
                cupom.getSituacao(),
                cupom.getOrigem()),
        itens,
        switch (status) {
          case "EM_TROCA" -> List.of("AUTORIZAR");
          case "TROCA_AUTORIZADA" -> List.of("RECEBER");
          default -> List.of();
        });
  }

  private Map<String, Object> snapshot(TrocaView t) {
    return Map.of(
        "codigo",
        t.codigo(),
        "pedidoId",
        t.pedidoId(),
        "status",
        t.status(),
        "integral",
        t.integral(),
        "itens",
        t.itens(),
        "cupomId",
        t.cupom() == null ? 0L : t.cupom().id(),
        "retornarEstoque",
        Boolean.TRUE.equals(t.retornarEstoque()));
  }

  private void registrarTransacao(TrocaView t) {
    repo.transacao(
        t.clienteId(),
        "TROCA",
        t.id(),
        t.codigo(),
        "TROCA",
        t.status(),
        t.itens().stream()
            .map(TrocaView.Item::valorLiquido)
            .reduce(BigDecimal.ZERO, BigDecimal::add),
        t.motivo(),
        reservas.agora());
  }

  private String impressao(Object in) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(json.writeValueAsBytes(in)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
