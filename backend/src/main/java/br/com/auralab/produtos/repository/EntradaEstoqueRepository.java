package br.com.auralab.produtos.repository;

import br.com.auralab.produtos.domain.EntradaEstoque;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntradaEstoqueRepository extends JpaRepository<EntradaEstoque, Long> {
  List<EntradaEstoque> findByProdutoIdOrderByDataEntradaDescIdDesc(Long produtoId);
}
