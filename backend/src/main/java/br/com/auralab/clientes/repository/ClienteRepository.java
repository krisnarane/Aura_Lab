package br.com.auralab.clientes.repository;

import br.com.auralab.clientes.domain.Cliente;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ClienteRepository
    extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {
  Optional<Cliente> findByCodigo(String codigo);

  boolean existsByCpfAndIdNot(String cpf, Long id);

  boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

  boolean existsByCpf(String cpf);

  boolean existsByEmailIgnoreCase(String email);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select c from Cliente c where c.id=:id")
  Optional<Cliente> buscarParaAtualizacao(Long id);

  @Query("select distinct c from Cliente c left join fetch c.enderecos where c.id in :ids")
  List<Cliente> carregarEnderecos(Collection<Long> ids);

  @Query(
      "select distinct c from Cliente c left join fetch c.cartoes ca left join fetch ca.bandeira where c.id in :ids")
  List<Cliente> carregarCartoes(Collection<Long> ids);
}
