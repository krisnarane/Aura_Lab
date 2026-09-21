package br.com.auralab.clientes.domain;

import java.math.BigDecimal;

/**
 * Strategy do ranking do cliente: permite trocar a fórmula sem tocar no service.
 *
 * <p>Requisito: RN0027 (ranking numérico baseado no perfil de compra); a fórmula concreta
 * é decisão do projeto, ver {@link PoliticaRankingPorCompras}.
 */
public interface PoliticaRankingCliente {

  /** Calcula o ranking (1 a 5) a partir do total de compras do cliente. */
  int calcular(BigDecimal totalCompras);
}
