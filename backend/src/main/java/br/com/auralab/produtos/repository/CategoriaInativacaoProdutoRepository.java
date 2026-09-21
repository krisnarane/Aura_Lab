package br.com.auralab.produtos.repository;

import br.com.auralab.produtos.domain.CategoriaInativacaoProduto;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório do domínio de categorias de inativação (RN0015); a chave é o código textual. */
public interface CategoriaInativacaoProdutoRepository
    extends JpaRepository<CategoriaInativacaoProduto, String> {}
