package br.com.auralab.clientes.domain;

import jakarta.persistence.*;

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
