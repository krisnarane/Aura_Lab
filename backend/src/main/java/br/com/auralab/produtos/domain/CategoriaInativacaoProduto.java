package br.com.auralab.produtos.domain;

import jakarta.persistence.*;

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
