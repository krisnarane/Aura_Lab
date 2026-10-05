package br.com.auralab.vendas.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Cupom usado no pagamento (RF0037): promocional (desconto sobre a compra, geral) ou de troca
 * (crédito vinculado a um cliente). Nesta fase os cupons usados são pré-carregados; o checkout
 * apenas gera o cupom de troco do RN0036 quando o valor dos cupons de troca supera o total da
 * compra. O código CUP- é gerado por trigger no banco.
 */
@Entity
public class Cupom {

  public static final String PROMOCIONAL = "PROMOCIONAL";
  public static final String TROCA = "TROCA";
  public static final String ATIVO = "ATIVO";
  public static final String UTILIZADO = "UTILIZADO";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, length = 20, insertable = false, updatable = false)
  private String codigo;

  @Column(nullable = false, length = 20)
  private String tipo;

  @Column(name = "cliente_id")
  private Long clienteId;

  @Column(precision = 12, scale = 2)
  private BigDecimal valor;

  @Column(precision = 5, scale = 2)
  private BigDecimal percentual;

  @Column(nullable = false, length = 20)
  private String situacao = ATIVO;

  private LocalDate validade;

  @Column(length = 120)
  private String origem;

  @Column(name = "criado_em", nullable = false)
  private OffsetDateTime criadoEm;

  protected Cupom() {}

  private Cupom(
      String tipo, Long clienteId, BigDecimal valor, BigDecimal percentual, String origem) {
    this.tipo = tipo;
    this.clienteId = clienteId;
    this.valor = valor;
    this.percentual = percentual;
    this.origem = origem;
    this.situacao = ATIVO;
    this.criadoEm = OffsetDateTime.now();
  }

  /** RN0036: cupom de troco gerado quando os cupons de troca superam o total da compra. */
  public static Cupom troco(Long clienteId, BigDecimal valor, String origem) {
    return new Cupom(TROCA, clienteId, valor, null, origem);
  }

  public boolean isTroca() {
    return TROCA.equals(tipo);
  }

  public boolean isPromocional() {
    return PROMOCIONAL.equals(tipo);
  }

  public boolean isAtivo() {
    return ATIVO.equals(situacao);
  }

  /** ATIVO e dentro da validade (quando houver) — base de "cupom válido" (RF0036/RF0037). */
  public boolean valido(LocalDate hoje) {
    return isAtivo() && (validade == null || !validade.isBefore(hoje));
  }

  public void utilizar() {
    this.situacao = UTILIZADO;
  }

  public Long getId() {
    return id;
  }

  public String getCodigo() {
    return codigo;
  }

  public String getTipo() {
    return tipo;
  }

  public Long getClienteId() {
    return clienteId;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public BigDecimal getPercentual() {
    return percentual;
  }

  public String getSituacao() {
    return situacao;
  }

  public LocalDate getValidade() {
    return validade;
  }

  public String getOrigem() {
    return origem;
  }
}
