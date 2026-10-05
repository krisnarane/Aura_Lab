package br.com.auralab.vendas.api.dto;

/**
 * Entrada do resumo/pré-cálculo da compra (RF0034): informa o endereço (por id do perfil ou apenas
 * a UF de um endereço novo) e, opcionalmente, o código de um cupom promocional.
 */
public record ResumoInput(Long enderecoId, String estado, String cupomPromocional) {}
