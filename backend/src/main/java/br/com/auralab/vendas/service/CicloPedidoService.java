package br.com.auralab.vendas.service;

import static br.com.auralab.vendas.repository.CicloVendaRepository.*;

import br.com.auralab.clientes.service.*;
import br.com.auralab.vendas.api.dto.*;
import br.com.auralab.vendas.domain.*;
import br.com.auralab.vendas.repository.*;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF0039/RF0040/RF0053, RN0028/RN0037--40: ciclo administrativo do pedido. */
@Service
public class CicloPedidoService {
  private final PedidoRepository pedidos;
  private final CupomRepository cupons;
  private final CicloVendaRepository repo;
  private final ReservaEstoqueService reservas;
  private final VendaService vendas;
  private final AuditoriaService auditoria;
  private final OperadoraCartao operadora;
  @PersistenceContext private EntityManager em;

  public CicloPedidoService(
      PedidoRepository pedidos,
      CupomRepository cupons,
      CicloVendaRepository repo,
      ReservaEstoqueService reservas,
      VendaService vendas,
      AuditoriaService auditoria,
      OperadoraCartao operadora) {
    this.pedidos = pedidos;
    this.cupons = cupons;
    this.repo = repo;
    this.reservas = reservas;
    this.vendas = vendas;
    this.auditoria = auditoria;
    this.operadora = operadora;
  }

  @Transactional
  public List<PedidoView> listar(String codigo, Long cliente, String status) {
    return vendas.listarAdministrativos(vazio(codigo), cliente, vazio(status));
  }

  @Transactional
  public PedidoView buscar(Long id) {
    var p = exigir(id);
    return vendas.buscarPedido(p.getClienteId(), id);
  }

  @Transactional
  public PedidoView validar(Long id, ValidacaoPagamentoInput in) {
    repo.bloquearFluxo();
    var inicial = exigir(id);
    reservas.sincronizar(inicial.getClienteId());
    var p = exigir(id);
    exigirAtual(p);
    if (!"EM_PROCESSAMENTO".equals(p.getStatus())) {
      if ("APROVADA".equals(p.getStatus()) && "APROVADO".equals(in.resultadoSimulado()))
        return view(p);
      if ("REPROVADA".equals(p.getStatus())) return view(p);
      RegrasCicloVenda.exigirEstado(p.getStatus(), "EM_PROCESSAMENTO");
    }
    var antes = RegistroVenda.pedido(p);
    String motivo = null;
    var rs = repo.reservasPedido(id);
    if (rs.size() != p.getItens().size()
        || rs.stream()
            .anyMatch(
                r ->
                    !tempo(r, "vence_em").isAfter(reservas.agora())
                        || qtd(r, "estoque") < qtd(r, "quantidade")
                        || !Boolean.TRUE.equals(r.get("ativo"))
                        || !Boolean.TRUE.equals(r.get("visivel")))) motivo = "RESERVA_INVALIDA";
    if (p.getCupomPromocionalId() != null) {
      var promo = cupons.findById(p.getCupomPromocionalId()).orElse(null);
      if (promo == null || !promo.isPromocional() || (promo.getClienteId()!=null && !promo.getClienteId().equals(p.getClienteId())) || !promo.valido(reservas.agora().toLocalDate()))
        motivo = "CUPOM_INVALIDO";
    }
    List<Map<String, Object>> resultados = new ArrayList<>();
    for (var parcela : p.getPartes()) {
      if (PagamentoParte.CUPOM_TROCA.equals(parcela.getTipo())) {
        var c = cupons.findById(parcela.getCupomId()).orElse(null);
        if (c == null
            || !c.isTroca()
            || !p.getClienteId().equals(c.getClienteId())
            || !c.reservadoPara(p.getId(), reservas.agora().toLocalDate())
            || c.getValor().compareTo(parcela.getValor()) != 0) motivo = "CUPOM_INVALIDO";
      } else {
        String solicitado =
            in.resultadosPorParcela() == null
                ? in.resultadoSimulado()
                : in.resultadosPorParcela().getOrDefault(parcela.getId(), in.resultadoSimulado());
        String resultado = operadora.validar(parcela.getValor(), solicitado);
        repo.resultadoParcela(parcela.getId(), resultado);
        resultados.add(
            Map.of(
                "parcelaId", parcela.getId(), "resultado", resultado, "valor", parcela.getValor()));
        if (!"APROVADO".equals(resultado))
          motivo = "FALHA".equals(resultado) ? "OPERADORA_INDISPONIVEL" : "OPERADORA_RECUSOU";
      }
    }
    if(rs.stream().anyMatch(r -> !tempo(r,"vence_em").isAfter(reservas.agora()))) motivo="RESERVA_EXPIRADA";
    boolean aprovado = motivo == null;
    if (aprovado) {
      for (var item : p.getItens()) {
        repo.baixar(item.getProdutoId(), item.getQuantidade(), item.getId(), reservas.agora());
        auditoria.registrarComo(
            p.getClienteId(),
            "ADMIN_DEMO",
            reservas.agora(),
            "BAIXAR_ESTOQUE",
            "ITEM_PEDIDO",
            item.getId(),
            Map.of(
                "quantidadeFisica",
                qtd(
                    rs.stream()
                        .filter(r -> id(r, "item_pedido_id").equals(item.getId()))
                        .findFirst()
                        .orElseThrow(),
                    "estoque")),
            Map.of("produtoId", item.getProdutoId(), "quantidadeBaixada", item.getQuantidade()));
      }
      repo.encerrarPedido(id, "CONSUMIDA");
      for (var parcela : p.getPartes())
        if (parcela.getCupomId() != null) {
          var cupom = cupons.findById(parcela.getCupomId()).orElseThrow();
          cupom.utilizar();
          auditoria.registrarComo(
              p.getClienteId(),
              "ADMIN_DEMO",
              reservas.agora(),
              "CONSUMIR_CUPOM",
              "CUPOM",
              cupom.getId(),
              Map.of("situacao", "RESERVADO"),
              Map.of("situacao", "UTILIZADO"));
        }
      if (p.getTrocoPrevisto().signum() > 0) {
        var c =
            Cupom.troco(p.getClienteId(), p.getTrocoPrevisto(), "Troco do pedido " + p.getCodigo());
        c.definirInstante(reservas.agora());
        cupons.saveAndFlush(c);
        em.refresh(c);
        p.registrarTrocoGerado(c.getId());
        registrarCupom(p.getClienteId(), c);
      }
    } else {
      repo.encerrarPedido(id, "RESERVA_EXPIRADA".equals(motivo) ? "EXPIRADA" : "LIBERADA");
      for (var parcela : p.getPartes())
        if (parcela.getCupomId() != null) {
          var c = cupons.findById(parcela.getCupomId()).orElse(null);
          if (c != null && c.reservadoPara(id, java.time.LocalDate.MIN)) {
            c.liberar();
            auditoria.registrarComo(
                p.getClienteId(),
                "ADMIN_DEMO",
                reservas.agora(),
                "LIBERAR_CUPOM",
                "CUPOM",
                c.getId(),
                Map.of("situacao", "RESERVADO"),
                Map.of("situacao", "ATIVO"));
          }
        }
    }
    p.resultadoPagamento(aprovado, motivo);
    pedidos.flush();
    var depois = new LinkedHashMap<>(RegistroVenda.pedido(p));
    depois.put("resultadoOperadora", resultados);
    auditoria.registrarComo(
        p.getClienteId(),
        "ADMIN_DEMO",
        reservas.agora(),
        "VALIDAR_PAGAMENTO",
        "PEDIDO",
        id,
        antes,
        depois);
    registrarPedido(p);
    repo.transacao(
        p.getClienteId(),
        "PAGAMENTO",
        id,
        "PAG-" + id,
        "PAGAMENTO",
        p.getStatus(),
        p.getTotal(),
        "Operadora simulada; resultado " + p.getPagamentoStatus(),
        reservas.agora());
    return view(p);
  }

  @Transactional
  public PedidoView despachar(Long id) {
    return transicionar(id, "APROVADA", "EM_TRANSPORTE", "DESPACHAR_PEDIDO");
  }

  @Transactional
  public PedidoView entregar(Long id) {
    return transicionar(id, "EM_TRANSPORTE", "ENTREGUE", "CONFIRMAR_ENTREGA");
  }

  private PedidoView transicionar(Long id, String de, String para, String operacao) {
    repo.bloquearFluxo();
    var p = exigir(id);
    exigirAtual(p);
    if (para.equals(p.getStatus())) return view(p);
    var antes = RegistroVenda.pedido(p);
    p.transicionar(de, para);
    pedidos.flush();
    auditoria.registrarComo(
        p.getClienteId(),
        "ADMIN_DEMO",
        reservas.agora(),
        operacao,
        "PEDIDO",
        id,
        antes,
        RegistroVenda.pedido(p));
    registrarPedido(p);
    return view(p);
  }

  public void registrarPedido(Pedido p) {
    repo.transacao(
        p.getClienteId(),
        "PEDIDO",
        p.getId(),
        p.getCodigo(),
        "PEDIDO",
        p.getStatus(),
        p.getTotal(),
        "Pedido real; pagamento " + p.getPagamentoStatus(),
        reservas.agora());
  }

  public void registrarCupom(Long cliente, Cupom c) {
    auditoria.registrarComo(
        cliente,
        "SISTEMA",
        reservas.agora(),
        "GERAR_CUPOM",
        "CUPOM",
        c.getId(),
        null,
        Map.of("codigo", c.getCodigo(), "valor", c.getValor(), "origem", c.getOrigem()));
    repo.transacao(
        cliente,
        "CUPOM",
        c.getId(),
        c.getCodigo(),
        "CUPOM",
        "ATIVO",
        c.getValor(),
        c.getOrigem(),
        reservas.agora());
  }

  public Pedido exigir(Long id) {
    return pedidos
        .findById(id)
        .orElseThrow(
            () -> new RegraNegocioException("PEDIDO_NAO_ENCONTRADO", "Pedido não encontrado."));
  }

  public void exigirAtual(Pedido p) {
    if (p.isLegado())
      throw new RegraNegocioException(
          "PEDIDO_LEGADO", "Pedido legado disponível somente para consulta; conciliação pendente.");
  }

  private PedidoView view(Pedido p) {
    return vendas.buscarPedido(p.getClienteId(), p.getId());
  }

  private String vazio(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
