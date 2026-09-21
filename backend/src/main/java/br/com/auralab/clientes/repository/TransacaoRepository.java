package br.com.auralab.clientes.repository;

import br.com.auralab.clientes.domain.TransacaoCliente;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório das transações demonstrativas: lista, detalhe vinculado e totais para o ranking (RF0025 e RN0027). */
public interface TransacaoRepository extends JpaRepository<TransacaoCliente, Long> {
  interface TotalCompras {
    Long getClienteId();

    java.math.BigDecimal getTotal();
  }

  // Total considerado no ranking (RN0027): somente pedidos pagos/concluídos.
  @org.springframework.data.jpa.repository.Query(
      "select t.clienteId as clienteId, sum(t.valor) as total from TransacaoCliente t where t.clienteId in :ids and t.tipo='PEDIDO' and t.status in ('PAGAMENTO_REALIZADO','EM_TRANSITO','ENTREGUE') group by t.clienteId")
  List<TotalCompras> totaisCompras(java.util.Collection<Long> ids);

  List<TransacaoCliente> findByClienteIdOrderByOcorridaEmDesc(Long id);

  Optional<TransacaoCliente> findByIdAndClienteId(Long id, Long clienteId);
}
