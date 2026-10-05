package br.com.auralab.vendas.api;

import br.com.auralab.clientes.api.dto.CartaoView;
import br.com.auralab.vendas.api.dto.*;
import br.com.auralab.vendas.service.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST do módulo de vendas (camada MVC). Delega aos services; o vínculo dos recursos ao
 * cliente da URL é validado no service.
 *
 * <p>Cobre RF0031/RF0032 (carrinho), RF0033/RF0038 (realizar e finalizar compra), RF0034 (frete,
 * via resumo), RF0035/RF0036/RF0037 e RN0033–RN0036 (endereço, cartões e cupons na finalização).
 * {@link CartaoView} é reutilizado do módulo de clientes.
 */
@RestController
@RequestMapping("/api/v1")
public class VendaController {

  private final CarrinhoService carrinho;
  private final VendaService vendas;

  public VendaController(CarrinhoService carrinho, VendaService vendas) {
    this.carrinho = carrinho;
    this.vendas = vendas;
  }

  @GetMapping("/clientes/{id}/carrinho")
  CarrinhoView carrinho(@PathVariable Long id) {
    return carrinho.obter(id);
  }

  @PostMapping("/clientes/{id}/carrinho/itens")
  ResponseEntity<CarrinhoView> adicionarItem(
      @PathVariable Long id, @Valid @RequestBody ItemCarrinhoInput in) {
    return ResponseEntity.status(201).body(carrinho.adicionar(id, in));
  }

  @PutMapping("/clientes/{id}/carrinho/itens/{itemId}")
  CarrinhoView alterarItem(
      @PathVariable Long id, @PathVariable Long itemId, @Valid @RequestBody QuantidadeInput in) {
    return carrinho.alterarQuantidade(id, itemId, in);
  }

  @DeleteMapping("/clientes/{id}/carrinho/itens/{itemId}")
  CarrinhoView removerItem(@PathVariable Long id, @PathVariable Long itemId) {
    return carrinho.remover(id, itemId);
  }

  /** RF0034: resumo com subtotal, desconto do cupom promocional, frete e total. */
  @PostMapping("/clientes/{id}/carrinho/resumo")
  ResumoView resumo(@PathVariable Long id, @RequestBody ResumoInput in) {
    return vendas.resumo(id, in);
  }

  /** RF0033/RF0038: finaliza a compra e cria o pedido com status EM_PROCESSAMENTO. */
  @PostMapping("/clientes/{id}/pedidos")
  ResponseEntity<PedidoView> finalizar(
      @PathVariable Long id, @Valid @RequestBody CheckoutInput in) {
    PedidoView v = vendas.finalizar(id, in);
    return ResponseEntity.created(URI.create("/api/v1/clientes/" + id + "/pedidos/" + v.id()))
        .body(v);
  }

  @GetMapping("/clientes/{id}/pedidos")
  List<PedidoView> pedidos(@PathVariable Long id) {
    return vendas.listarPedidos(id);
  }

  @GetMapping("/clientes/{id}/pedidos/{pedidoId}")
  PedidoView pedido(@PathVariable Long id, @PathVariable Long pedidoId) {
    return vendas.buscarPedido(id, pedidoId);
  }

  @GetMapping("/clientes/{id}/cupons")
  List<CupomView> cupons(@PathVariable Long id) {
    return vendas.cuponsDoCliente(id);
  }
}
