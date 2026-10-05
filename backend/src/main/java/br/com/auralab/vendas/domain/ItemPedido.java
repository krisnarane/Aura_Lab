package br.com.auralab.vendas.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Item de um pedido: snapshot do produto (código, nome e preço unitário) no momento da compra, com
 * a quantidade e o subtotal calculado. O snapshot preserva o histórico mesmo que o cadastro do
 * produto mude depois.
 */
@Entity
@Table(name = "item_pedido")
public class ItemPedido {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pedido_id")
  private Pedido pedido;

  @Column(name = "produto_id", nullable = false)
  private Long produtoId;

  @Column(name = "produto_codigo", nullable = false, length = 20)
  private String produtoCodigo;

  @Column(name = "produto_nome", nullable = false, length = 150)
  private String produtoNome;

  @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
  private BigDecimal precoUnitario;

  @Column(nullable = false)
  private int quantidade;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotal;

  protected ItemPedido() {}

  public ItemPedido(
      Long produtoId,
      String produtoCodigo,
      String produtoNome,
      BigDecimal precoUnitario,
      int quantidade) {
    this.produtoId = produtoId;
    this.produtoCodigo = produtoCodigo;
    this.produtoNome = produtoNome;
    this.precoUnitario = precoUnitario;
    this.quantidade = quantidade;
    this.subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
  }

  void vincular(Pedido p) {
    pedido = p;
  }

  public Long getId() {
    return id;
  }

  public Long getProdutoId() {
    return produtoId;
  }

  public String getProdutoCodigo() {
    return produtoCodigo;
  }

  public String getProdutoNome() {
    return produtoNome;
  }

  public BigDecimal getPrecoUnitario() {
    return precoUnitario;
  }

  public int getQuantidade() {
    return quantidade;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }
}
