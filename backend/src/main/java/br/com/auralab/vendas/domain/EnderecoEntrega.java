package br.com.auralab.vendas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Snapshot do endereço de entrega copiado para o pedido (RN0023). Preserva a composição usada na
 * compra mesmo que o endereço do perfil mude ou seja inativado depois. Aplica-se tanto a um
 * endereço selecionado do perfil quanto a um novo endereço informado no checkout.
 *
 * <p>Guarda os nove campos obrigatórios da RN0023 e também o campo opcional {@code observacoes}
 * previsto na mesma regra.
 */
@Embeddable
public class EnderecoEntrega {

  @Column(name = "entrega_apelido", length = 60)
  private String apelido;

  @Column(name = "entrega_tipo_residencia", nullable = false, length = 40)
  private String tipoResidencia;

  @Column(name = "entrega_tipo_logradouro", nullable = false, length = 40)
  private String tipoLogradouro;

  @Column(name = "entrega_logradouro", nullable = false, length = 160)
  private String logradouro;

  @Column(name = "entrega_numero", nullable = false, length = 20)
  private String numero;

  @Column(name = "entrega_bairro", nullable = false, length = 100)
  private String bairro;

  @Column(name = "entrega_cep", nullable = false, length = 8)
  private String cep;

  @Column(name = "entrega_cidade", nullable = false, length = 100)
  private String cidade;

  @Column(name = "entrega_estado", nullable = false, length = 60)
  private String estado;

  @Column(name = "entrega_pais", nullable = false, length = 60)
  private String pais;

  @Column(name = "entrega_complemento", length = 100)
  private String complemento;

  @Column(name = "entrega_observacoes", length = 500)
  private String observacoes;

  protected EnderecoEntrega() {}

  public EnderecoEntrega(
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
      String complemento,
      String observacoes) {
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
    this.complemento = complemento;
    this.observacoes = observacoes;
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
}
