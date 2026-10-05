package br.com.auralab.vendas.service;

import br.com.auralab.vendas.domain.*;
import java.util.*;

/**
 * Snapshots de auditoria de vendas com campos explicitamente permitidos (RNF0012). Nunca serializa
 * dados sensíveis de cartão: as parcelas guardam apenas a descrição mascarada (bandeira e quatro
 * últimos dígitos) montada pelo service.
 */
public final class RegistroVenda {
  private RegistroVenda() {}

  /**
   * Snapshot completo dos itens do carrinho, no mesmo formato de coleção usado por {@code
   * RegistroCliente.enderecos/cartoes}. É o que permite guardar "os dados alterados" exigidos pela
   * RNF0012: capturado antes e depois da mutação, mostra a quantidade anterior de cada item, não
   * apenas a resultante.
   */
  public static Map<String, Object> carrinho(Carrinho cart) {
    return Map.of(
        "itens",
        cart.getItens().stream()
            .map(
                i -> {
                  Map<String, Object> m = new LinkedHashMap<>();
                  m.put("itemId", i.getId());
                  m.put("produtoId", i.getProdutoId());
                  m.put("quantidade", i.getQuantidade());
                  return m;
                })
            .toList());
  }

  public static Map<String, Object> pedido(Pedido p) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("codigo", p.getCodigo());
    m.put("status", p.getStatus());
    m.put("subtotal", p.getSubtotal());
    m.put("desconto", p.getDesconto());
    m.put("frete", p.getFrete());
    m.put("total", p.getTotal());
    m.put("entregaCidade", p.getEntrega().getCidade());
    m.put("entregaEstado", p.getEntrega().getEstado());
    m.put(
        "itens",
        p.getItens().stream()
            .map(
                i -> {
                  Map<String, Object> x = new LinkedHashMap<>();
                  x.put("produtoId", i.getProdutoId());
                  x.put("quantidade", i.getQuantidade());
                  x.put("precoUnitario", i.getPrecoUnitario());
                  return x;
                })
            .toList());
    m.put(
        "pagamentos",
        p.getPartes().stream()
            .map(
                parte -> {
                  Map<String, Object> x = new LinkedHashMap<>();
                  x.put("tipo", parte.getTipo());
                  x.put("descricao", parte.getDescricao());
                  x.put("valor", parte.getValor());
                  return x;
                })
            .toList());
    if (p.getCupomTrocaGeradoId() != null) m.put("cupomTrocaGeradoId", p.getCupomTrocaGeradoId());
    return m;
  }
}
