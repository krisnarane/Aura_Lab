package br.com.auralab.produtos.domain;

import jakarta.persistence.*;

/**
 * Tabela de domínio das categorias de ativação de produto (RN0017: ex.
 * RETORNO_AO_MERCADO, REPOSICAO_ESTOQUE, CORRECAO_CADASTRO). Alimentada pela migração V5
 * (RNF0013); o código é a chave natural.
 */
@Entity
@Table(name = "categoria_ativacao_produto")
public class CategoriaAtivacaoProduto {

  @Id
  @Column(length = 40)
  private String codigo;

  @Column(nullable = false, length = 500)
  private String descricao;

  protected CategoriaAtivacaoProduto() {}

  public String getCodigo() {
    return codigo;
  }

  public String getDescricao() {
    return descricao;
  }
}
