package br.com.auralab.produtos.repository;

import br.com.auralab.produtos.domain.CategoriaAtivacaoProduto;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório do domínio de categorias de ativação (RN0017); a chave é o código textual. */
public interface CategoriaAtivacaoProdutoRepository
    extends JpaRepository<CategoriaAtivacaoProduto, String> {}
