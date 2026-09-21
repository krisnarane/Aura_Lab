package br.com.auralab.produtos.api.dto;

import jakarta.validation.constraints.*;

/** Corpo da ativação/inativação: categoria de domínio e justificativa obrigatórias (RN0015/RN0017). */
public record MotivoStatusProdutoInput(
    @NotBlank @Size(max = 40) String categoria, @NotBlank String justificativa) {}
