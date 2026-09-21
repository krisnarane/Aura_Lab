package br.com.auralab.produtos.domain;

import jakarta.persistence.*;

/**
 * Tabela de domínio das categorias de inativação de produto (RN0015; RN0016 reserva FORA
 * DE MERCADO para a inativação automática, ainda não coberta — RF0013). Alimentada pela
 * migração V5 (RNF0013); o código é a chave natural.
 */
@Entity
@Table(name = "categoria_inativacao_produto")
public class CategoriaInativacaoProduto {

  @Id
  @Column(length = 40)
  private String codigo;

  @Column(nullable = false, length = 500)
  private String descricao;

  protected CategoriaInativacaoProduto() {}

  public String getCodigo() {
    return codigo;
  }

  public String getDescricao() {
    return descricao;
  }
}
