package br.com.auralab.vendas.service;

import java.math.BigDecimal;

/** Porta da operadora acadêmica; não recebe PAN nem CVV. */
public interface OperadoraCartao {
  String validar(Long cartaoId, BigDecimal valor);
}
