package br.com.auralab.vendas.domain;

import java.math.BigDecimal;

/**
 * Estratégia de cálculo de frete (RF0034). O DRS não define a fórmula; a política concreta é uma
 * decisão do projeto, explicada na apresentação.
 */
public interface PoliticaFrete {

  /**
   * @param estado UF do endereço de entrega
   * @param totalUnidades soma das quantidades dos itens
   * @param subtotal soma dos itens antes de desconto e frete
   * @return valor do frete (nunca negativo)
   */
  BigDecimal calcular(String estado, int totalUnidades, BigDecimal subtotal);
}
