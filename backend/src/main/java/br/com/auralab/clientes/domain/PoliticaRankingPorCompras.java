package br.com.auralab.clientes.domain;

import java.math.*;
import org.springframework.stereotype.Component;

/**
 * Implementação atual da Strategy de ranking (RN0027):
 * {@code min(5, 1 + floor(total / 200))} — fórmula definida pelo projeto, não pelo DRS.
 */
@Component
public class PoliticaRankingPorCompras implements PoliticaRankingCliente {

  /** Total negativo é tratado como zero; ranking nunca passa de 5 nem fica abaixo de 1. */
  public int calcular(BigDecimal total) {
    return total
            .max(BigDecimal.ZERO)
            .divideToIntegralValue(BigDecimal.valueOf(200))
            .min(BigDecimal.valueOf(4))
            .intValue()
        + 1;
  }
}
