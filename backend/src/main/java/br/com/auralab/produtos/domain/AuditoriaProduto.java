package br.com.auralab.produtos.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "auditoria_produto")
public class AuditoriaProduto {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "produto_id")
  private Long produtoId;

  @Column(name = "ocorrida_em")
  private OffsetDateTime ocorridaEm;

  private String ator, operacao, alteracoes, entidade;

  @Column(name = "entidade_id")
  private Long entidadeId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "dados_anteriores")
  private Map<String, Object> dadosAnteriores;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "dados_novos")
  private Map<String, Object> dadosNovos;

  protected AuditoriaProduto() {}

  public AuditoriaProduto(
      Long produtoId,
      String operacao,
      String entidade,
      Long entidadeId,
      Map<String, Object> antes,
      Map<String, Object> depois) {
    this.produtoId = produtoId;
    this.operacao = operacao;
    this.entidade = entidade;
    this.entidadeId = entidadeId;
    this.dadosAnteriores = antes;
    this.dadosNovos = depois;
    this.ator = "ADMIN_DEMO";
    this.ocorridaEm = OffsetDateTime.now();
    this.alteracoes = operacao + " — " + entidade + " #" + entidadeId;
  }

  public Long getId() {
    return id;
  }

  public OffsetDateTime getOcorridaEm() {
    return ocorridaEm;
  }

  public String getAtor() {
    return ator;
  }

  public String getOperacao() {
    return operacao;
  }

  public String getAlteracoes() {
    return alteracoes;
  }

  public String getEntidade() {
    return entidade;
  }

  public Long getEntidadeId() {
    return entidadeId;
  }

  public Map<String, Object> getDadosAnteriores() {
    return dadosAnteriores;
  }

  public Map<String, Object> getDadosNovos() {
    return dadosNovos;
  }
}
