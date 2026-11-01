package br.com.auralab.vendas.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * Motivos possíveis de reprovação da validação do pagamento (RF0039, RN0037–RN0040).
 *
 * <p>Uma mesma validação pode encontrar mais de uma violação — por exemplo, reserva vencida e
 * cartão recusado. A ordem de declaração <b>é</b> a precedência adotada, da causa mais estrutural
 * (que impediria a compra independentemente do pagamento) para a mais específica do meio de
 * pagamento:
 *
 * <ol>
 *   <li>{@link #RESERVA_EXPIRADA} — o prazo de reserva venceu; nenhuma outra causa é acionável e é
 *       ela que determina o encerramento das reservas como {@code EXPIRADA}.
 *   <li>{@link #RESERVA_INVALIDA} — reserva, estoque ou situação do produto inconsistentes.
 *   <li>{@link #CUPOM_INVALIDO} — crédito promocional ou de troca indisponível para o pedido.
 *   <li>{@link #OPERADORA_INDISPONIVEL} — falha técnica da operadora, passível de nova tentativa.
 *   <li>{@link #OPERADORA_RECUSOU} — recusa da operadora para o cartão informado.
 * </ol>
 *
 * <p>A precedência existe para que o motivo registrado em {@code pedido.motivo_resultado} não
 * dependa da ordem em que as checagens aparecem no código. É coberta por {@code
 * MotivoReprovacaoTest}.
 */
public enum MotivoReprovacao {
  RESERVA_EXPIRADA,
  RESERVA_INVALIDA,
  CUPOM_INVALIDO,
  OPERADORA_INDISPONIVEL,
  OPERADORA_RECUSOU;

  /**
   * Motivo de maior precedência entre as violações encontradas. O resultado vazio significa que
   * nenhuma violação foi encontrada, ou seja, pagamento aprovado.
   */
  public static Optional<MotivoReprovacao> prioritario(Collection<MotivoReprovacao> violacoes) {
    return violacoes.stream().min(Comparator.naturalOrder());
  }
}
