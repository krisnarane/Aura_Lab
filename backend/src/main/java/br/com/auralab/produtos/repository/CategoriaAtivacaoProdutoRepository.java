package br.com.auralab.produtos.repository;

import br.com.auralab.produtos.domain.CategoriaAtivacaoProduto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaAtivacaoProdutoRepository
    extends JpaRepository<CategoriaAtivacaoProduto, String> {}
