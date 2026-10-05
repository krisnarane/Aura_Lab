package br.com.auralab.vendas.api.dto;

import jakarta.validation.constraints.*;

/** Entrada para adicionar um item ao carrinho (RF0031/RF0032): produto e quantidade. */
public record ItemCarrinhoInput(@NotNull Long produtoId, @NotNull @Min(1) Integer quantidade) {}
