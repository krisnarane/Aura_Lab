package br.com.auralab.clientes.repository;

import br.com.auralab.clientes.domain.TransacaoCliente;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório das transações demonstrativas: lista, detalhe vinculado e totais para o ranking
 * (RF0025 e RN0027).
 */
public interface TransacaoRepository extends JpaRepository<TransacaoCliente, Long> {
  interface TotalCompras {
    Long getClienteId();

    java.math.BigDecimal getTotal();
  }

  // Total considerado no ranking (RN0027): somente pedidos pagos/concluídos.
  @org.springframework.data.jpa.repository.Query(
      value =
          "select t.cliente_id as clienteId, sum(t.valor) as total from transacao_cliente t left join pedido p on t.origem_entidade='PEDIDO' and t.origem_id=p.id where t.cliente_id in (:ids) and t.tipo='PEDIDO' and ((t.origem='LEGADO' and t.status in ('PAGAMENTO_REALIZADO','EM_TRANSITO','ENTREGUE')) or (t.origem='REAL' and p.pagamento_status='APROVADO')) group by t.cliente_id",
      nativeQuery = true)
  List<TotalCompras> totaisCompras(java.util.Collection<Long> ids);

  List<TransacaoCliente> findByClienteIdOrderByOcorridaEmDesc(Long id);

  Optional<TransacaoCliente> findByIdAndClienteId(Long id, Long clienteId);
}
