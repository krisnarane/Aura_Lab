package br.com.auralab.vendas.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Parcela da composição de pagamento do pedido (RF0037): um valor alocado a um cartão de crédito
 * ({@code CARTAO}) ou o valor de face de um cupom de troca ({@code CUPOM_TROCA}). O cupom
 * promocional entra como desconto no pedido, não como parcela. Nenhum dado sensível de cartão
 * (PAN/CVV) é guardado — apenas a descrição mascarada.
 */
@Entity
@Table(name = "pagamento_parte")
public class PagamentoParte {

  public static final String CARTAO = "CARTAO";
  public static final String CUPOM_TROCA = "CUPOM_TROCA";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pedido_id")
  private Pedido pedido;

  @Column(nullable = false, length = 20)
  private String tipo;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal valor;

  @Column(nullable = false, length = 160)
  private String descricao;

  @Column(name = "cartao_id")
  private Long cartaoId;

  @Column(name = "cupom_id")
  private Long cupomId;

  protected PagamentoParte() {}

  private PagamentoParte(
      String tipo, BigDecimal valor, String descricao, Long cartaoId, Long cupomId) {
    this.tipo = tipo;
    this.valor = valor;
    this.descricao = descricao;
    this.cartaoId = cartaoId;
    this.cupomId = cupomId;
  }

  public static PagamentoParte cartao(BigDecimal valor, String descricao, Long cartaoId) {
    return new PagamentoParte(CARTAO, valor, descricao, cartaoId, null);
  }

  public static PagamentoParte cupomTroca(BigDecimal valor, String descricao, Long cupomId) {
    return new PagamentoParte(CUPOM_TROCA, valor, descricao, null, cupomId);
  }

  void vincular(Pedido p) {
    pedido = p;
  }

  public Long getId() {
    return id;
  }

  public String getTipo() {
    return tipo;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public String getDescricao() {
    return descricao;
  }

  public Long getCartaoId() {
    return cartaoId;
  }

  public Long getCupomId() {
    return cupomId;
  }
}
