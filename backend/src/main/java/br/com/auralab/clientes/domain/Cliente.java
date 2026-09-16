package br.com.auralab.clientes.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
public class Cliente {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, length = 20, insertable = false, updatable = false)
  private String codigo;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(nullable = false, unique = true, length = 11)
  private String cpf;

  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @Column(nullable = false, length = 40)
  private String genero;

  @Column(nullable = false)
  private LocalDate nascimento;

  @Column(name = "telefone_tipo", nullable = false, length = 20)
  private String telefoneTipo;

  @Column(name = "telefone_ddd", nullable = false, length = 2)
  private String telefoneDdd;

  @Column(name = "telefone_numero", nullable = false, length = 9)
  private String telefoneNumero;

  @Column(name = "senha_hash", nullable = false, length = 100)
  private String senhaHash;

  @Column(nullable = false)
  private boolean ativo = true;

  @Column(name = "criado_em", nullable = false)
  private OffsetDateTime criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;

  @org.hibernate.annotations.BatchSize(size = 100)
  @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
  @OrderBy("id")
  private List<Endereco> enderecos = new ArrayList<>();

  @org.hibernate.annotations.BatchSize(size = 100)
  @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
  @OrderBy("id")
  private List<Cartao> cartoes = new ArrayList<>();

  protected Cliente() {}

  public Cliente(
      String nome,
      String cpf,
      String email,
      String genero,
      LocalDate nascimento,
      String telefoneTipo,
      String telefoneDdd,
      String telefoneNumero,
      String senhaHash) {

    this.nome = nome;
    this.cpf = cpf;
    this.email = email;
    this.genero = genero;
    this.nascimento = nascimento;

    this.telefoneTipo = telefoneTipo;
    this.telefoneDdd = telefoneDdd;
    this.telefoneNumero = telefoneNumero;
    this.senhaHash = senhaHash;

    this.criadoEm = OffsetDateTime.now();
    this.atualizadoEm = this.criadoEm;
  }

  public void alterar(
      String nome,
      String cpf,
      String email,
      String genero,
      LocalDate nascimento,
      String telefoneTipo,
      String telefoneDdd,
      String telefoneNumero) {

    this.nome = nome;
    this.cpf = cpf;
    this.email = email;
    this.genero = genero;
    this.nascimento = nascimento;
    this.telefoneTipo = telefoneTipo;
    this.telefoneDdd = telefoneDdd;
    this.telefoneNumero = telefoneNumero;
    this.atualizadoEm = OffsetDateTime.now();
  }

  public void inativar() {
    this.ativo = false;
    this.atualizadoEm = OffsetDateTime.now();
  }

  public void alterarSenha(String hash) {
    this.senhaHash = hash;
    this.atualizadoEm = OffsetDateTime.now();
  }

  public void adicionarEndereco(Endereco e) {
    e.vincular(this);
    enderecos.add(e);
  }

  public void adicionarCartao(Cartao c) {
    c.vincular(this);
    cartoes.add(c);
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

  public String getCpf() {
    return cpf;
  }

  public String getEmail() {
    return email;
  }

  public String getGenero() {
    return genero;
  }

  public LocalDate getNascimento() {
    return nascimento;
  }

  public String getTelefoneTipo() {
    return telefoneTipo;
  }

  public String getTelefoneDdd() {
    return telefoneDdd;
  }

  public String getTelefoneNumero() {
    return telefoneNumero;
  }

  public String getSenhaHash() {
    return senhaHash;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public List<Endereco> getEnderecos() {
    return Collections.unmodifiableList(enderecos);
  }

  public List<Cartao> getCartoes() {
    return Collections.unmodifiableList(cartoes);
  }
}
