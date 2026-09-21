package br.com.auralab.clientes.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/**
 * Entidade de cliente: dados pessoais, senha (somente hash BCrypt) e situação.
 *
 * <p>Requisitos: RF0021 (cadastro), RF0022 (alteração), RF0023 (inativação lógica — nunca
 * exclusão física), RF0028 (alteração apenas de senha), RN0026 (dados obrigatórios),
 * RNF0033 (senha criptografada) e RNF0035 (código CLI- único).
 */
@Entity
public class Cliente {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Código CLI- gerado por trigger no banco; imutável após a gravação (RNF0035).

  @Column(unique = true, length = 20, insertable = false, updatable = false)
  private String codigo;

  @Column(nullable = false, length = 150)
  private String nome;

  // CPF e e-mail únicos no banco reforçam a unicidade exigida por RN0026.
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

  // Texto puro nunca é persistido: só o hash BCrypt (RNF0033); nunca expor em respostas.
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

  /** Inativação lógica do cadastro (RF0023); permanece no banco sem reativação nesta etapa. */
  public void inativar() {
    this.ativo = false;
    this.atualizadoEm = OffsetDateTime.now();
  }

  /** Alteração isolada de senha (RF0028); recebe o hash já calculado pelo service. */
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
