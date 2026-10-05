package br.com.auralab.vendas.api.dto;

import java.math.BigDecimal;

/** Saída de um item do carrinho com preço, quantidade, subtotal e estoque disponível. */
public record ItemCarrinhoView(
    Long id,
    Long produtoId,
    String codigo,
    String nome,
    BigDecimal precoUnitario,
    int quantidade,
    BigDecimal subtotal,
    int estoqueDisponivel,
    boolean disponivel) {}
