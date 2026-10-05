package br.com.auralab.vendas.api.dto;

import br.com.auralab.clientes.api.dto.EnderecoInput;
import jakarta.validation.Valid;
import java.util.List;

/**
 * Entrada da finalização da compra (RF0033/RF0038). Endereço de entrega por id do perfil ({@code
 * enderecoId}) ou novo endereço ({@code novoEndereco}) com opção de incorporar (RF0035/RN0023).
 * Pagamento com cupom promocional (código, RN0033), cupons de troca (ids, RF0037) e cartões com
 * valores alocados (RF0036/RN0034/RN0035).
 */
public record CheckoutInput(
    Long enderecoId,
    @Valid EnderecoInput novoEndereco,
    boolean incorporarEndereco,
    String cupomPromocional,
    List<Long> cuponsTroca,
    List<@Valid ParcelaCartaoInput> cartoes) {}
