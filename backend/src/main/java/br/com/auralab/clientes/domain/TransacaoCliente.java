package br.com.auralab.clientes.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;

@Entity
@Table(name = "transacao_cliente")
public class TransacaoCliente {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cliente_id")
  private Long clienteId;

  private String codigo;
  private String tipo;
  private String status;
  private BigDecimal valor;

  @Column(name = "ocorrida_em")
  private OffsetDateTime ocorridaEm;

  private String detalhes;

  protected TransacaoCliente() {}

  public Long getId() {
    return id;
  }

  public Long getClienteId() {
    return clienteId;
  }

  public String getCodigo() {
    return codigo;
  }

  public String getTipo() {
    return tipo;
  }

  public String getStatus() {
    return status;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public OffsetDateTime getOcorridaEm() {
    return ocorridaEm;
  }

  public String getDetalhes() {
    return detalhes;
  }
}
