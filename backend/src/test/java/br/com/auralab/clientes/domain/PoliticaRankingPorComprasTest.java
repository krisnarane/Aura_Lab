package br.com.auralab.clientes.domain;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PoliticaRankingPorComprasTest {
  @ParameterizedTest
  @CsvSource({
    "0,1",
    "199.99,1",
    "200,2",
    "399.99,2",
    "400,3",
    "600,4",
    "799.99,4",
    "800,5",
    "999999999999,5",
    "-100,1"
  })
  void RN0027_faixas(String total, int esperado) {
    assertThat(new PoliticaRankingPorCompras().calcular(new BigDecimal(total))).isEqualTo(esperado);
  }
}
