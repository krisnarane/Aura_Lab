package br.com.auralab.produtos.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
public class Produto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, length = 20, insertable = false, updatable = false)
  private String codigo;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(nullable = false, length = 80)
  private String marca;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal preco;

  @Column(nullable = false)
  private int estoque;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(nullable = false)
  private boolean visivel = true;

  @Column(name = "categoria_inativacao", length = 40)
  private String categoriaInativacao;

  @Column(name = "justificativa_inativacao")
  private String justificativaInativacao;

  @Column(name = "inativado_em")
  private OffsetDateTime inativadoEm;

  @Column(name = "categoria_ativacao", length = 40)
  private String categoriaAtivacao;

  @Column(name = "justificativa_ativacao")
  private String justificativaAtivacao;

  @Column(name = "ativado_em")
  private OffsetDateTime ativadoEm;

  @Column(name = "criado_em", nullable = false)
  private OffsetDateTime criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;

  protected Produto() {}

  public Produto(String nome, String marca, BigDecimal preco, int estoque, boolean visivel) {
    this.nome = nome;
    this.marca = marca;
    this.preco = preco;
    this.estoque = estoque;
    this.visivel = visivel;
    this.criadoEm = OffsetDateTime.now();
    this.atualizadoEm = this.criadoEm;
  }

  public void inativar(String categoria, String justificativa) {
    this.ativo = false;
    this.visivel = false;
    this.categoriaInativacao = categoria;
    this.justificativaInativacao = justificativa;
    this.inativadoEm = OffsetDateTime.now();
    this.atualizadoEm = this.inativadoEm;
  }

  public void ativar(String categoria, String justificativa) {
    this.ativo = true;
    this.categoriaAtivacao = categoria;
    this.justificativaAtivacao = justificativa;
    this.ativadoEm = OffsetDateTime.now();
    this.atualizadoEm = this.ativadoEm;
  }

  public Long getId() {
    return id;
  }

  public String getCodigo() {
    return codigo;
  }

  public String getNome() {
    return nome;
  }

  public String getMarca() {
    return marca;
  }

  public BigDecimal getPreco() {
    return preco;
  }

  public int getEstoque() {
    return estoque;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public boolean isVisivel() {
    return visivel;
  }

  public String getCategoriaInativacao() {
    return categoriaInativacao;
  }

  public String getJustificativaInativacao() {
    return justificativaInativacao;
  }

  public OffsetDateTime getInativadoEm() {
    return inativadoEm;
  }

  public String getCategoriaAtivacao() {
    return categoriaAtivacao;
  }

  public String getJustificativaAtivacao() {
    return justificativaAtivacao;
  }

  public OffsetDateTime getAtivadoEm() {
    return ativadoEm;
  }
}
