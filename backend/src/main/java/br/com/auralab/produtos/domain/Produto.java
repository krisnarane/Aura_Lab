package br.com.auralab.produtos.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Produto da loja (livro no DRS). Situação ({@code ativo}) e visibilidade ({@code
 * visivel}) são independentes: inativar força {@code visivel = false} e ativar não
 * republica automaticamente. Guarda o último motivo de ativação e de inativação; o
 * histórico completo fica em {@code auditoria_produto}.
 *
 * <p>Requisitos: RF0011/RF0014 (cadastro e edição), RF0012/RN0015 (inativar com motivo), RF0016/
 * RN0017 (ativar com motivo) e RNF0021 (código PRD- único gerado por trigger no banco).
 */
@Entity
public class Produto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Código PRD-###### gerado por trigger no banco; único e imutável (RNF0021).
  @Column(unique = true, length = 20, insertable = false, updatable = false)
  private String codigo;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(nullable = false, length = 80)
  private String marca;

  @Column(nullable = false, columnDefinition = "text")
  private String descricao;

  @Column(name = "imagem_url", nullable = false, columnDefinition = "text")
  private String imagemUrl;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "produto_categoria", joinColumns = @JoinColumn(name = "produto_id"), inverseJoinColumns = @JoinColumn(name = "categoria_codigo"))
  private Set<CategoriaProduto> categorias = new LinkedHashSet<>();

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

  public void editar(String nome, String marca, String descricao, String imagemUrl,
      BigDecimal preco, int estoque, boolean visivel, Set<CategoriaProduto> categorias) {
    this.nome = nome;
    this.marca = marca;
    this.descricao = descricao;
    this.imagemUrl = imagemUrl;
    this.preco = preco;
    this.estoque = estoque;
    this.visivel = visivel;
    this.categorias.clear();
    this.categorias.addAll(categorias);
    this.atualizadoEm = OffsetDateTime.now();
  }

  public String getDescricao() { return descricao; }
  public String getImagemUrl() { return imagemUrl; }
  public Set<CategoriaProduto> getCategorias() { return categorias; }

  /** RF0012/RN0015: inativação lógica com motivo obrigatório; também oculta o produto. */
  public void inativar(String categoria, String justificativa) {
    this.ativo = false;
    this.visivel = false;
    this.categoriaInativacao = categoria;
    this.justificativaInativacao = justificativa;
    this.inativadoEm = OffsetDateTime.now();
    this.atualizadoEm = this.inativadoEm;
  }

  /** RF0016/RN0017: ativação lógica com motivo obrigatório; não altera a visibilidade. */
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
