package br.com.auralab.produtos.repository;

import br.com.auralab.produtos.domain.CategoriaInativacaoProduto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaInativacaoProdutoRepository
    extends JpaRepository<CategoriaInativacaoProduto, String> {}
