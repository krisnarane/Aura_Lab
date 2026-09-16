package br.com.auralab.produtos.domain;

import jakarta.persistence.*;

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
