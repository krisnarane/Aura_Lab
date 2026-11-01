package br.com.auralab.produtos.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** RF0051: registro histórico imutável da entrada, sem fornecedor (RN0050). */
@Entity
@Table(name = "entrada_estoque")
public class EntradaEstoque {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(optional = false) @JoinColumn(name = "produto_id") private Produto produto;
  private int quantidade;
  @Column(name = "valor_custo") private BigDecimal valorCusto;
  @Column(name = "data_entrada") private LocalDate dataEntrada;
  @Column(name = "preco_calculado") private BigDecimal precoCalculado;
  @Column(name = "registrada_em") private OffsetDateTime registradaEm;
  protected EntradaEstoque() {}
  public EntradaEstoque(Produto produto, int quantidade, BigDecimal valorCusto, LocalDate dataEntrada,
      BigDecimal precoCalculado, OffsetDateTime registradaEm) {
    this.produto = produto; this.quantidade = quantidade; this.valorCusto = valorCusto;
    this.dataEntrada = dataEntrada; this.precoCalculado = precoCalculado; this.registradaEm = registradaEm;
  }
  public Long getId() { return id; }
  public int getQuantidade() { return quantidade; }
  public BigDecimal getValorCusto() { return valorCusto; }
  public LocalDate getDataEntrada() { return dataEntrada; }
  public BigDecimal getPrecoCalculado() { return precoCalculado; }
  public OffsetDateTime getRegistradaEm() { return registradaEm; }
}
