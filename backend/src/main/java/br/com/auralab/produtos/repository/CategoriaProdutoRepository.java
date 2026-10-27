package br.com.auralab.produtos.repository;

import br.com.auralab.produtos.domain.CategoriaProduto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaProdutoRepository extends JpaRepository<CategoriaProduto, String> {}
