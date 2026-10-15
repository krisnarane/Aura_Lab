package br.com.auralab.vendas.service;

import br.com.auralab.clientes.service.RegraNegocioException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Operadora acadêmica: não recebe PAN/CVV, não cobra valores reais. */
@Component
public class GatewayPagamentoSimulado implements OperadoraCartao {
  public String validar(BigDecimal valor, String resultado) {
    if (!java.util.Set.of("APROVADO", "REPROVADO", "FALHA").contains(resultado))
      throw new RegraNegocioException("SIMULACAO_INVALIDA", "Resultado de operadora inválido.");
    if (valor.signum() <= 0)
      throw new RegraNegocioException("PAGAMENTO_INVALIDO", "Valor de cartão deve ser positivo.");
    return resultado;
  }
}
