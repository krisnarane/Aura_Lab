package br.com.auralab.vendas.api.dto;

import java.math.BigDecimal;

/** Saída de uma parcela de pagamento: tipo, descrição mascarada e valor. */
public record PagamentoParteView(String tipo, String descricao, BigDecimal valor) {}
