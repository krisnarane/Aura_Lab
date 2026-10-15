package br.com.auralab.vendas.api;

import br.com.auralab.vendas.api.dto.*;
import br.com.auralab.vendas.service.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Atores demonstrativos; não representa autenticação ou autorização real. */
@RestController
@RequestMapping("/api/v1")
public class CicloVendaController {
  private final CicloPedidoService pedidos;
  private final TrocaService trocas;
  private final ReservaEstoqueService reservas;

  public CicloVendaController(
      CicloPedidoService pedidos, TrocaService trocas, ReservaEstoqueService reservas) {
    this.pedidos = pedidos;
    this.trocas = trocas;
    this.reservas = reservas;
  }

  @GetMapping("/admin/pedidos")
  List<PedidoView> pedidos(
      @RequestParam(required = false) String codigo,
      @RequestParam(required = false) Long clienteId,
      @RequestParam(required = false) String status) {
    return pedidos.listar(codigo, clienteId, status);
  }

  @GetMapping("/admin/pedidos/{id}")
  PedidoView pedido(@PathVariable Long id) {
    return pedidos.buscar(id);
  }

  @PostMapping("/admin/pedidos/{id}/pagamento/validar")
  PedidoView validar(@PathVariable Long id, @Valid @RequestBody ValidacaoPagamentoInput in) {
    return pedidos.validar(id, in);
  }

  @PostMapping("/admin/pedidos/{id}/despacho")
  PedidoView despachar(@PathVariable Long id) {
    return pedidos.despachar(id);
  }

  @PostMapping("/admin/pedidos/{id}/entrega")
  PedidoView entregar(@PathVariable Long id) {
    return pedidos.entregar(id);
  }

  @PostMapping("/clientes/{cliente}/pedidos/{pedido}/trocas")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  TrocaView solicitar(
      @PathVariable Long cliente,
      @PathVariable Long pedido,
      @Valid @RequestBody SolicitacaoTrocaInput in,
      @RequestHeader(value = "Idempotency-Key", required = false) String chave) {
    return trocas.solicitar(cliente, pedido, in, chave);
  }

  @GetMapping("/clientes/{cliente}/trocas")
  List<TrocaView> trocasCliente(
      @PathVariable Long cliente, @RequestParam(required = false) Long pedidoId) {
    return trocas.listar(cliente, pedidoId);
  }

  @GetMapping("/clientes/{cliente}/trocas/{id}")
  TrocaView trocaCliente(@PathVariable Long cliente, @PathVariable Long id) {
    return trocas.buscar(cliente, id);
  }

  @GetMapping("/admin/trocas")
  List<TrocaView> trocasAdmin(
      @RequestParam(required = false) Long clienteId,
      @RequestParam(required = false) Long pedidoId) {
    return trocas.listar(clienteId, pedidoId);
  }

  @PostMapping("/admin/trocas/{id}/autorizacao")
  TrocaView autorizar(@PathVariable Long id) {
    return trocas.autorizar(id);
  }

  @PostMapping("/admin/trocas/{id}/recebimento")
  TrocaView receber(@PathVariable Long id, @Valid @RequestBody RecebimentoTrocaInput in) {
    return trocas.receber(id, in.retornarEstoque());
  }

  @GetMapping("/clientes/{cliente}/notificacoes")
  List<NotificacaoVendaView> notificacoes(@PathVariable Long cliente) {
    reservas.sincronizar(cliente);
    return reservas.notificacoes(cliente);
  }
}
