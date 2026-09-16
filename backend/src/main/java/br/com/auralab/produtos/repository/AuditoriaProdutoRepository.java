package br.com.auralab.produtos.repository;

import br.com.auralab.produtos.domain.AuditoriaProduto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaProdutoRepository extends JpaRepository<AuditoriaProduto, Long> {
  List<AuditoriaProduto> findByProdutoIdOrderByOcorridaEmDesc(Long produtoId);
}
