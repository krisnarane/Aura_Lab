package br.com.auralab.vendas.api.dto;

import jakarta.validation.constraints.*;

/** Entrada para alterar a quantidade de um item do carrinho (RF0032). */
public record QuantidadeInput(@NotNull @Min(1) Integer quantidade) {}
