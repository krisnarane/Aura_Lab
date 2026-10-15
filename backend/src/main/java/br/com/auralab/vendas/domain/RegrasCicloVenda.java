package br.com.auralab.vendas.domain;

import br.com.auralab.clientes.service.RegraNegocioException;
import java.math.*;
import java.util.*;

/** Estados do DRS e rateio em centavos da política de troca escolhida pelo grupo. */
public final class RegrasCicloVenda {
  private RegrasCicloVenda() {}

  public static void exigirEstado(String atual, String esperado) {
    if (!esperado.equals(atual))
      throw new RegraNegocioException(
          "ESTADO_VENDA_INVALIDO", "A operação exige o estado " + esperado.replace('_', ' ') + ".");
  }

  public static List<Long> ratearDesconto(List<BigDecimal> subtotais, BigDecimal desconto) {
    long total = subtotais.stream().mapToLong(v -> v.movePointRight(2).longValueExact()).sum();
    long centavos = desconto.movePointRight(2).longValueExact();
    List<Long> partes = new ArrayList<>();
    long usado = 0;
    for (var subtotal : subtotais) {
      long parte =
          total == 0
              ? 0
              : BigDecimal.valueOf(centavos)
                  .multiply(subtotal.movePointRight(2))
                  .divide(BigDecimal.valueOf(total), 0, RoundingMode.DOWN)
                  .longValueExact();
      partes.add(parte);
      usado += parte;
    }
    for (int i = 0; usado < centavos; i = (i + 1) % partes.size()) {
      partes.set(i, partes.get(i) + 1);
      usado++;
    }
    return partes;
  }

  public static BigDecimal creditoParcial(
      BigDecimal subtotal, long descontoCentavos, int unidades, int comprometidas, int quantidade) {
    long liquido = subtotal.movePointRight(2).longValueExact() - descontoCentavos;
    long base = liquido / unidades, resto = liquido % unidades;
    long bonus = Math.max(0, Math.min(resto, comprometidas + quantidade) - comprometidas);
    return BigDecimal.valueOf(base * quantidade + bonus, 2);
  }
}
