package br.com.auralab.vendas.repository;

import br.com.auralab.vendas.domain.Carrinho;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

/** Repositório do carrinho: um por cliente, com bloqueio pessimista para serializar edições. */
public interface CarrinhoRepository extends JpaRepository<Carrinho, Long> {

  Optional<Carrinho> findByClienteId(Long clienteId);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Carrinho c where c.clienteId = :clienteId")
  Optional<Carrinho> buscarParaAtualizacao(Long clienteId);
}
