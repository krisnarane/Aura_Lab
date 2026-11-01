package br.com.auralab.vendas.repository;

import br.com.auralab.vendas.domain.Pedido;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Repositório de pedidos: consulta por cliente e por id validando a associação ao cliente. */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

  List<Pedido> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

  Optional<Pedido> findByIdAndClienteId(Long id, Long clienteId);

  /**
   * Projeção escalar do cliente do pedido. Serve para descobrir quem sincronizar sem trazer a
   * entidade para o contexto de persistência antes da conciliação, que pode invalidá-lo.
   */
  @Query("select p.clienteId from Pedido p where p.id = ?1")
  Optional<Long> clienteIdDoPedido(Long id);
}
