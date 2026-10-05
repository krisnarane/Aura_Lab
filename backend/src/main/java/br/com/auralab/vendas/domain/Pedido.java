package br.com.auralab.vendas.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.hibernate.annotations.BatchSize;

/**
 * Pedido criado na finalização da compra (RF0038). Nesta fase o pedido nasce com status {@code
 * EM_PROCESSAMENTO}; a validação/aprovação do pagamento e as transições seguintes estão fora do
 * escopo. Guarda os totais (subtotal, desconto do cupom promocional, frete e total), a composição
 * de pagamento (partes) e o snapshot do endereço de entrega.
 */
@Entity
public class Pedido {

  public static final String EM_PROCESSAMENTO = "EM_PROCESSAMENTO";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, length = 20, insertable = false, updatable = false)
  private String codigo;

  @Column(name = "cliente_id", nullable = false)
  private Long clienteId;

  @Column(nullable = false, length = 30)
  private String status;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotal;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal desconto;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal frete;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total;

  @Column(name = "cupom_promocional_id")
  private Long cupomPromocionalId;

  @Column(name = "cupom_troca_gerado_id")
  private Long cupomTrocaGeradoId;

  @Column(name = "entrega_endereco_id")
  private Long entregaEnderecoId;

  @Embedded private EnderecoEntrega entrega;

  @Column(name = "criado_em", nullable = false)
  private OffsetDateTime criadoEm;

  @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  @BatchSize(size = 100)
  private List<ItemPedido> itens = new ArrayList<>();

  @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  @BatchSize(size = 100)
  private List<PagamentoParte> partes = new ArrayList<>();

  protected Pedido() {}

  public Pedido(
      Long clienteId,
      BigDecimal subtotal,
      BigDecimal desconto,
      BigDecimal frete,
      BigDecimal total,
      Long cupomPromocionalId,
      Long entregaEnderecoId,
      EnderecoEntrega entrega) {
    this.clienteId = clienteId;
    this.status = EM_PROCESSAMENTO;
    this.subtotal = subtotal;
    this.desconto = desconto;
    this.frete = frete;
    this.total = total;
    this.cupomPromocionalId = cupomPromocionalId;
    this.entregaEnderecoId = entregaEnderecoId;
    this.entrega = entrega;
    this.criadoEm = OffsetDateTime.now();
  }

  public void adicionarItem(ItemPedido item) {
    item.vincular(this);
    itens.add(item);
  }

  public void adicionarParte(PagamentoParte parte) {
    parte.vincular(this);
    partes.add(parte);
  }

  public void registrarTrocoGerado(Long cupomId) {
    this.cupomTrocaGeradoId = cupomId;
  }

  public Long getId() {
    return id;
  }

  public String getCodigo() {
    return codigo;
  }

  public Long getClienteId() {
    return clienteId;
  }

  public String getStatus() {
    return status;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public BigDecimal getDesconto() {
    return desconto;
  }

  public BigDecimal getFrete() {
    return frete;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public Long getCupomPromocionalId() {
    return cupomPromocionalId;
  }

  public Long getCupomTrocaGeradoId() {
    return cupomTrocaGeradoId;
  }

  public Long getEntregaEnderecoId() {
    return entregaEnderecoId;
  }

  public EnderecoEntrega getEntrega() {
    return entrega;
  }

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public List<ItemPedido> getItens() {
    return Collections.unmodifiableList(itens);
  }

  public List<PagamentoParte> getPartes() {
    return Collections.unmodifiableList(partes);
  }
}
