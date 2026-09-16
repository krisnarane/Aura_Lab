package br.com.auralab.clientes.domain;

import java.math.BigDecimal;

public interface PoliticaRankingCliente {
  int calcular(BigDecimal totalCompras);
}
