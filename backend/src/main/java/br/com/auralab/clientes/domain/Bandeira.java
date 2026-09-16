package br.com.auralab.clientes.domain;

import jakarta.persistence.*;

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
