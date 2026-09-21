package br.com.auralab.clientes.domain;

import jakarta.persistence.*;

/**
 * Endereço do cliente com apelido, composição postal, finalidades (cobrança/entrega) e
 * preferência de entrega.
 *
 * <p>Requisitos: RF0026 (múltiplos endereços de entrega), RN0021/RN0022 (ao menos um
 * endereço de cobrança e um de entrega), RN0023 (campos obrigatórios), RNF0034
 * (alterar/adicionar sem editar o cadastro inteiro) e RF0023 (inativação lógica).
 */
@Entity
public class Endereco {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id")
  private Cliente cliente;

  private String apelido;

  @Column(name = "tipo_residencia")
  private String tipoResidencia;

  @Column(name = "tipo_logradouro")
  private String tipoLogradouro;

  private String logradouro;
  private String numero;
  private String bairro;
  private String cep;
  private String cidade;
  private String estado;
  private String pais;
  private String observacoes;
  private String complemento;
  private boolean cobranca;
  private boolean entrega;

  @Column(name = "preferencial_entrega")
  private boolean preferencialEntrega;

  private boolean ativo = true;

  protected Endereco() {}

  public Endereco(
      String apelido,
      String tipoResidencia,
      String tipoLogradouro,
      String logradouro,
      String numero,
      String bairro,
      String cep,
      String cidade,
      String estado,
      String pais,
      String observacoes,
      String complemento,
      boolean cobranca,
      boolean entrega,
      boolean preferencialEntrega) {
    alterar(
        apelido,
        tipoResidencia,
        tipoLogradouro,
        logradouro,
        numero,
        bairro,
        cep,
        cidade,
        estado,
        pais,
        observacoes,
        complemento,
        cobranca,
        entrega,
        preferencialEntrega);
  }

  void vincular(Cliente c) {
    cliente = c;
  }

  public void alterar(
      String apelido,
      String tipoResidencia,
      String tipoLogradouro,
      String logradouro,
      String numero,
      String bairro,
      String cep,
      String cidade,
      String estado,
      String pais,
      String observacoes,
      String complemento,
      boolean cobranca,
      boolean entrega,
      boolean preferencialEntrega) {
    this.apelido = apelido;
    this.tipoResidencia = tipoResidencia;
    this.tipoLogradouro = tipoLogradouro;
    this.logradouro = logradouro;
    this.numero = numero;
    this.bairro = bairro;
    this.cep = cep;
    this.cidade = cidade;
    this.estado = estado;
    this.pais = pais;
    this.observacoes = observacoes;
    this.complemento = complemento;
    this.cobranca = cobranca;
    this.entrega = entrega;
    this.preferencialEntrega = preferencialEntrega;
  }

  /** Inativação lógica; um endereço inativo não pode continuar preferencial. */
  public void inativar() {
    ativo = false;
    preferencialEntrega = false;
  }

  public void definirPreferencial(boolean v) {
    preferencialEntrega = v;
  }

  public Long getId() {
    return id;
  }

  public String getApelido() {
    return apelido;
  }

  public String getTipoResidencia() {
    return tipoResidencia;
  }

  public String getTipoLogradouro() {
    return tipoLogradouro;
  }

  public String getLogradouro() {
    return logradouro;
  }

  public String getNumero() {
    return numero;
  }

  public String getBairro() {
    return bairro;
  }

  public String getCep() {
    return cep;
  }

  public String getCidade() {
    return cidade;
  }

  public String getEstado() {
    return estado;
  }

  public String getPais() {
    return pais;
  }

  public String getComplemento() {
    return complemento;
  }

  public String getObservacoes() {
    return observacoes;
  }

  public boolean isCobranca() {
    return cobranca;
  }

  public boolean isEntrega() {
    return entrega;
  }

  public boolean isPreferencialEntrega() {
    return preferencialEntrega;
  }

  public boolean isAtivo() {
    return ativo;
  }
}
