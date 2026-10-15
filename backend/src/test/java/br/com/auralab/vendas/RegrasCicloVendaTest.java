package br.com.auralab.vendas;

import static org.assertj.core.api.Assertions.*;

import br.com.auralab.vendas.domain.RegrasCicloVenda;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegrasCicloVendaTest {
  @Test
  void RF0045_rateioDeDescontoEUnidadesPreservaCentavosHistoricos() {
    var partes =
        RegrasCicloVenda.ratearDesconto(
            List.of(new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("10.00")),
            new BigDecimal("1.00"));
    assertThat(partes).containsExactly(34L, 33L, 33L);
    BigDecimal soma = BigDecimal.ZERO;
    for (int i = 0; i < 3; i++)
      soma = soma.add(RegrasCicloVenda.creditoParcial(new BigDecimal("10.00"), 34L, 3, i, 1));
    assertThat(soma).isEqualByComparingTo("9.66");
    assertThat(RegrasCicloVenda.creditoParcial(new BigDecimal("10.00"), 34L, 3, 0, 3))
        .isEqualByComparingTo(soma);
  }

  @Test
  void RN0039_RN0040_RN0041_recusaTransicaoIncompativel() {
    assertThatThrownBy(() -> RegrasCicloVenda.exigirEstado("REPROVADA", "APROVADA"))
        .hasMessageContaining("APROVADA");
    assertThatCode(() -> RegrasCicloVenda.exigirEstado("ENTREGUE", "ENTREGUE"))
        .doesNotThrowAnyException();
  }
}
