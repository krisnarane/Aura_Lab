package br.com.auralab.clientes.api.dto;

import jakarta.validation.constraints.*;

/** Entrada da troca de senha (RF0028) com confirmação obrigatória (RNF0032). */
public record SenhaInput(@NotBlank String novaSenha, @NotBlank String confirmacaoSenha) {}
