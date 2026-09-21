package br.com.auralab.produtos.repository;

import br.com.auralab.produtos.domain.Produto;
import java.util.*;
import org.springframework.data.jpa.repository.*;

/** Repositório de produtos: consulta com filtros (RF0015) e bloqueio pessimista para o ciclo ativar/inativar. */
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

  // Bloqueio pessimista para serializar ativações/inativações concorrentes (RF0012/RF0016).
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Produto p where p.id=:id")
  Optional<Produto> buscarParaAtualizacao(Long id);

  // RF0015: parâmetros nulos são ignorados; escape '!' preserva % e _ literais no nome.
  @Query(
      """
  select p from Produto p
  where (:codigo is null or p.codigo = :codigo)
    and (:nome is null or lower(p.nome) like :nome escape '!')
    and (:ativo is null or p.ativo = :ativo)
  order by p.nome, p.id
  """)
  List<Produto> filtrar(String codigo, String nome, Boolean ativo);
}
