package br.com.auralab.vendas.repository;

import br.com.auralab.vendas.domain.Pedido;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório de pedidos: consulta por cliente e por id validando a associação ao cliente. */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

  List<Pedido> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

  Optional<Pedido> findByIdAndClienteId(Long id, Long clienteId);
}
