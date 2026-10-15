package br.com.auralab.vendas.service;

import java.math.BigDecimal;

/** Porta da operadora, com dados mascarados e resposta controlada na demonstração. */
public interface OperadoraCartao {
  String validar(BigDecimal valor, String resultado);
}
