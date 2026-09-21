package br.com.auralab.clientes.domain;

import jakarta.persistence.*;

/**
 * Cartão de crédito do cliente: titular, bandeira persistida, últimos quatro dígitos e
 * marca de preferencial.
 *
 * <p>Requisitos: RF0027 (cadastro de cartões, exatamente um preferencial), RN0024 (o
 * número completo e o CVV são descartados após a validação; só restam os últimos quatro
 * dígitos) e RN0025 (bandeira precisa existir no sistema).
 */
@Entity
public class Cartao {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id")
  private Cliente cliente;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "bandeira_id")
  private Bandeira bandeira;

  private String titular;

  // PAN e CVV jamais são persistidos — apenas os últimos quatro dígitos (RN0024).
  @Column(name = "ultimos_quatro")
  private String ultimosQuatro;

  private boolean preferencial;
  private boolean ativo = true;

  protected Cartao() {}

  public Cartao(Bandeira b, String t, String u, boolean p) {
    bandeira = b;
    titular = t;
    ultimosQuatro = u;
    preferencial = p;
  }

  void vincular(Cliente c) {
    cliente = c;
  }

  public void definirPreferencial(boolean v) {
    preferencial = v;
  }

  public void inativar() {
    ativo = false;
    preferencial = false;
  }

  public Long getId() {
    return id;
  }

  public Bandeira getBandeira() {
    return bandeira;
  }

  public String getTitular() {
    return titular;
  }

  public String getUltimosQuatro() {
    return ultimosQuatro;
  }

  public boolean isPreferencial() {
    return preferencial;
  }

  public boolean isAtivo() {
    return ativo;
  }
}
