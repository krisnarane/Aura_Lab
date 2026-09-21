package br.com.auralab.clientes.domain;

import jakarta.persistence.*;

/**
 * Tabela de domínio de bandeiras de cartão aceitas pela loja.
 *
 * <p>Requisitos: RN0025 (a bandeira precisa estar cadastrada no sistema) e RNF0013
 * (tabelas de domínio alimentadas por script de implantação — neste projeto, migrações
 * Flyway).
 */
@Entity
public class Bandeira {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true)
  private String nome;

  protected Bandeira() {}

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }
}
