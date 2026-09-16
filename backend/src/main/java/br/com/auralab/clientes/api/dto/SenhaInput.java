package br.com.auralab.clientes.api.dto;

import jakarta.validation.constraints.*;

public record SenhaInput(@NotBlank String novaSenha, @NotBlank String confirmacaoSenha) {}
