package br.com.auralab.produtos.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "categoria_produto")
public class CategoriaProduto {
  @Id private String codigo;
  private String descricao;
  protected CategoriaProduto() {}
  public String getCodigo() { return codigo; }
  public String getDescricao() { return descricao; }
}
