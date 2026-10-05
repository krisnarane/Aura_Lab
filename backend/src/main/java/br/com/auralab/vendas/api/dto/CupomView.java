package br.com.auralab.vendas.api.dto;

import java.math.BigDecimal;

/** Saída de cupom: código, tipo, valor/percentual, situação e origem. */
public record CupomView(
    Long id,
    String codigo,
    String tipo,
    BigDecimal valor,
    BigDecimal percentual,
    String situacao,
    String origem) {}
