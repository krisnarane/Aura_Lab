package br.com.auralab.vendas.domain;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Política de frete adotada pelo projeto (RF0034), pensada para ser simples e explicável na
 * apresentação:
 *
 * <ul>
 *   <li>base por região da UF de entrega: Sudeste (SP, RJ, MG, ES) = R$ 15,00; Sul (PR, SC, RS) =
 *       R$ 20,00; demais estados = R$ 30,00;
 *   <li>acréscimo de R$ 2,00 por unidade de item (manuseio/volume);
 *   <li>frete grátis quando o subtotal atinge R$ 300,00.
 * </ul>
 *
 * O critério considera os itens selecionados (quantidade) e o endereço informado (UF), como exige a
 * RF0034.
 */
@Component
public class PoliticaFretePorRegiao implements PoliticaFrete {

  private static final BigDecimal SUDESTE = new BigDecimal("15.00");
  private static final BigDecimal SUL = new BigDecimal("20.00");
  private static final BigDecimal OUTROS = new BigDecimal("30.00");
  private static final BigDecimal POR_UNIDADE = new BigDecimal("2.00");
  private static final BigDecimal FRETE_GRATIS_A_PARTIR_DE = new BigDecimal("300.00");
  private static final Set<String> UF_SUDESTE = Set.of("SP", "RJ", "MG", "ES");
  private static final Set<String> UF_SUL = Set.of("PR", "SC", "RS");

  @Override
  public BigDecimal calcular(String estado, int totalUnidades, BigDecimal subtotal) {
    if (subtotal != null && subtotal.compareTo(FRETE_GRATIS_A_PARTIR_DE) >= 0)
      return BigDecimal.ZERO.setScale(2);
    String uf = estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
    BigDecimal base = UF_SUDESTE.contains(uf) ? SUDESTE : UF_SUL.contains(uf) ? SUL : OUTROS;
    return base.add(POR_UNIDADE.multiply(BigDecimal.valueOf(Math.max(0, totalUnidades))))
        .setScale(2);
  }
}
