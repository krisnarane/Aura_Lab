package br.com.auralab.vendas.api.dto;

import jakarta.validation.constraints.*;
import java.util.Map;

public record ValidacaoPagamentoInput(
    @NotBlank @Pattern(regexp = "APROVADO|REPROVADO|FALHA") String resultadoSimulado,
    Map<Long, String> resultadosPorParcela) {}
