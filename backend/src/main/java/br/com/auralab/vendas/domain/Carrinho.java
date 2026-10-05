package br.com.auralab.vendas.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Carrinho de compra: repositório temporário de itens de um cliente (RF0031). Há no máximo um
 * carrinho por cliente; itens são adicionados, alterados, removidos e visualizados respeitando o
 * estoque disponível (RN0031). O bloqueio/expiração de itens (RN0044/RN0045) está fora do escopo
 * desta fase.
 */
@Entity
public class Carrinho {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cliente_id", nullable = false, unique = true)
  private Long clienteId;

  @Column(name = "criado_em", nullable = false)
  private OffsetDateTime criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;

  @OneToMany(mappedBy = "carrinho", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private List<ItemCarrinho> itens = new ArrayList<>();

  protected Carrinho() {}

  public Carrinho(Long clienteId) {
    this.clienteId = clienteId;
    this.criadoEm = OffsetDateTime.now();
    this.atualizadoEm = this.criadoEm;
  }

  public ItemCarrinho item(Long produtoId) {
    return itens.stream().filter(i -> i.getProdutoId().equals(produtoId)).findFirst().orElse(null);
  }

  public ItemCarrinho itemPorId(Long itemId) {
    return itens.stream().filter(i -> i.getId().equals(itemId)).findFirst().orElse(null);
  }

  public void adicionar(ItemCarrinho item) {
    item.vincular(this);
    itens.add(item);
    tocar();
  }

  public void remover(ItemCarrinho item) {
    itens.remove(item);
    tocar();
  }

  public void tocar() {
    this.atualizadoEm = OffsetDateTime.now();
  }

  public void esvaziar() {
    itens.clear();
    tocar();
  }

  public Long getId() {
    return id;
  }

  public Long getClienteId() {
    return clienteId;
  }

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public OffsetDateTime getAtualizadoEm() {
    return atualizadoEm;
  }

  public List<ItemCarrinho> getItens() {
    return Collections.unmodifiableList(itens);
  }
}
