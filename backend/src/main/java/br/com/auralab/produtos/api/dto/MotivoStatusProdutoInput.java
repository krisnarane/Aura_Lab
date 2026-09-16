package br.com.auralab.produtos.api.dto;

import jakarta.validation.constraints.*;

public record MotivoStatusProdutoInput(
    @NotBlank @Size(max = 40) String categoria, @NotBlank String justificativa) {}
