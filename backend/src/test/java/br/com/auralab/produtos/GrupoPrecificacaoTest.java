package br.com.auralab.produtos;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.auralab.produtos.domain.GrupoPrecificacao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GrupoPrecificacaoTest {
  @Test void RF0052_margemEArredondamento() {
    assertThat(new GrupoPrecificacao("P", "Padrão", new BigDecimal("50"))
        .precoDeVenda(new BigDecimal("33.33"))).isEqualByComparingTo("50.00");
    assertThat(new GrupoPrecificacao("X", "Premium", new BigDecimal("80"))
        .precoDeVenda(new BigDecimal("10.00"))).isEqualByComparingTo("18.00");
    assertThat(new GrupoPrecificacao("Z", "Zero", BigDecimal.ZERO)
        .precoDeVenda(BigDecimal.ZERO)).isEqualByComparingTo("0.00");
  }
}
