package br.com.auralab.vendas.api.dto;

import java.math.BigDecimal;

/** Saída de um item do pedido (snapshot do produto no momento da compra). */
public record ItemPedidoView(
    Long produtoId,
    String codigo,
    String nome,
    BigDecimal precoUnitario,
    int quantidade,
    BigDecimal subtotal) {}
