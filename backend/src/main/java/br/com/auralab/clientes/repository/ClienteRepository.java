package br.com.auralab.clientes.repository;

import br.com.auralab.clientes.domain.Cliente;
import java.util.*;
import org.springframework.data.jpa.repository.*;

/**
 * Repositório de clientes: consultas derivadas de unicidade (RN0026), busca dinâmica via
 * Specifications (RF0024) e carregamento em lote de endereços/cartões para a listagem
 * (apoio a RNF0011).
 */
public interface ClienteRepository
    extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {
  Optional<Cliente> findByCodigo(String codigo);

  boolean existsByCpfAndIdNot(String cpf, Long id);

  boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

  boolean existsByCpf(String cpf);

  boolean existsByEmailIgnoreCase(String email);

  // Bloqueio pessimista: serializa escritas concorrentes na mesma linha do cliente.
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select c from Cliente c where c.id=:id")
  Optional<Cliente> buscarParaAtualizacao(Long id);

  @Query("select distinct c from Cliente c left join fetch c.enderecos where c.id in :ids")
  List<Cliente> carregarEnderecos(Collection<Long> ids);

  @Query(
      "select distinct c from Cliente c left join fetch c.cartoes ca left join fetch ca.bandeira where c.id in :ids")
  List<Cliente> carregarCartoes(Collection<Long> ids);
}
