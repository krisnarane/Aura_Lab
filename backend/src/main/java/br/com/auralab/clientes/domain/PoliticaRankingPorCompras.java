package br.com.auralab.clientes.domain;

import java.math.*;
import org.springframework.stereotype.Component;

@Component
public class PoliticaRankingPorCompras implements PoliticaRankingCliente {
  public int calcular(BigDecimal total) {
    return total
            .max(BigDecimal.ZERO)
            .divideToIntegralValue(BigDecimal.valueOf(200))
            .min(BigDecimal.valueOf(4))
            .intValue()
        + 1;
  }
}
