package br.com.auralab.vendas.api.dto;

import br.com.auralab.clientes.api.dto.CartaoInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Uma parcela de pagamento em cartão (RF0036/RN0034/RN0035): usa um cartão do perfil ({@code
 * cartaoId}) ou um cartão novo informado na compra ({@code novoCartao}), com a opção de
 * incorporá-lo ao perfil, e o valor alocado a esse cartão.
 */
public record ParcelaCartaoInput(
    Long cartaoId,
    @Valid CartaoInput novoCartao,
    boolean incorporarCartao,
    @NotNull @DecimalMin(value = "0.00") BigDecimal valor) {}
