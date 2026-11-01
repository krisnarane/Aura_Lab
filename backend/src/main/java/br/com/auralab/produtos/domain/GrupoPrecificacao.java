package br.com.auralab.produtos.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** RN0013/RF0052: margem controlada aplicada ao custo da última entrada. */
@Entity
@Table(name = "grupo_precificacao")
public class GrupoPrecificacao {
  @Id private String codigo;
  private String descricao;
  @Column(name = "percentual_margem", nullable = false, precision = 5, scale = 2)
  private BigDecimal percentualMargem;

  protected GrupoPrecificacao() {}
  public GrupoPrecificacao(String codigo, String descricao, BigDecimal percentualMargem) {
    this.codigo = codigo;
    this.descricao = descricao;
    this.percentualMargem = percentualMargem;
  }
  public BigDecimal precoDeVenda(BigDecimal custo) {
    return custo.multiply(BigDecimal.ONE.add(percentualMargem.movePointLeft(2)))
        .setScale(2, RoundingMode.HALF_UP);
  }
  public String getCodigo() { return codigo; }
  public String getDescricao() { return descricao; }
  public BigDecimal getPercentualMargem() { return percentualMargem; }
}
