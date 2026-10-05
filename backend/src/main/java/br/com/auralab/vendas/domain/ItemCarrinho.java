package br.com.auralab.vendas.domain;

import jakarta.persistence.*;

/**
 * Item do carrinho: referencia um produto e a quantidade desejada (RF0031/RF0032). A validação de
 * estoque (RN0031) é feita no service ao adicionar ou alterar a quantidade.
 */
@Entity
@Table(name = "item_carrinho")
public class ItemCarrinho {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "carrinho_id")
  private Carrinho carrinho;

  @Column(name = "produto_id", nullable = false)
  private Long produtoId;

  @Column(nullable = false)
  private int quantidade;

  protected ItemCarrinho() {}

  public ItemCarrinho(Long produtoId, int quantidade) {
    this.produtoId = produtoId;
    this.quantidade = quantidade;
  }

  void vincular(Carrinho c) {
    carrinho = c;
  }

  public void definirQuantidade(int quantidade) {
    this.quantidade = quantidade;
  }

  public Long getId() {
    return id;
  }

  public Long getProdutoId() {
    return produtoId;
  }

  public int getQuantidade() {
    return quantidade;
  }
}
