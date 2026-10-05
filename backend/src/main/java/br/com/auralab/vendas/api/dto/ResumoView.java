package br.com.auralab.vendas.api.dto;

import java.math.BigDecimal;

/** Saída do resumo: subtotal, desconto do cupom promocional, frete e total a pagar. */
public record ResumoView(
    BigDecimal subtotal,
    int totalUnidades,
    BigDecimal desconto,
    BigDecimal frete,
    BigDecimal total) {}
