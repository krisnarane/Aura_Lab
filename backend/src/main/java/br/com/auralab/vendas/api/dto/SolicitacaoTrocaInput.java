package br.com.auralab.vendas.api.dto;

import jakarta.validation.constraints.*;

public record SolicitacaoTrocaInput(
    boolean integral,
    Long itemPedidoId,
    @Min(1) Integer quantidade,
    @NotBlank @Size(max = 500) String motivo) {}
