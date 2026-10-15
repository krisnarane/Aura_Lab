package br.com.auralab.vendas.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TrocaView(
    Long id,
    String codigo,
    Long pedidoId,
    Long clienteId,
    boolean integral,
    String status,
    String motivo,
    OffsetDateTime criadaEm,
    Boolean retornarEstoque,
    CupomView cupom,
    List<Item> itens,
    List<String> acoesPermitidas) {
  public record Item(
      Long itemPedidoId,
      Long produtoId,
      String produtoNome,
      int quantidade,
      BigDecimal valorLiquido) {}
}
