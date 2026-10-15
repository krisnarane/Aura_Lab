package br.com.auralab.vendas.api.dto;

import jakarta.validation.constraints.NotNull;

public record RecebimentoTrocaInput(@NotNull Boolean retornarEstoque) {}
