package br.com.auralab.vendas.service;

import br.com.auralab.clientes.service.RegraNegocioException;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Operadora acadêmica: não recebe PAN/CVV, não cobra valores reais. */
@Component
public class GatewayPagamentoSimulado implements OperadoraCartao {
  private final JdbcTemplate jdbc;

  public GatewayPagamentoSimulado(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public String validar(Long cartaoId, BigDecimal valor) {
    if (valor.signum() <= 0)
      throw new RegraNegocioException("PAGAMENTO_INVALIDO", "Valor de cartão deve ser positivo.");
    if (cartaoId == null) return "APROVADO";
    return jdbc.query(
        "select resultado from resultado_operadora_cartao where cartao_id=?",
        rs -> rs.next() ? rs.getString(1) : "APROVADO",
        cartaoId);
  }
}
