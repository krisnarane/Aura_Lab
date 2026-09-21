package br.com.auralab.clientes.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Registro de auditoria das escritas sobre o cliente: instante, ator, operação, entidade e
 * snapshots JSON antes/depois permitidos (sem senha, PAN ou CVV).
 *
 * <p>Requisito: RNF0012 (toda escrita registra data, hora, usuário responsável e dados
 * alterados). O ator é o técnico {@code ADMIN_DEMO} — não há usuário autenticado nesta
 * etapa.
 */
@Entity
@Table(name = "auditoria_cliente")
public class AuditoriaCliente {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cliente_id")
  private Long clienteId;

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

  protected AuditoriaCliente() {}

  public AuditoriaCliente(
      Long clienteId,
      String operacao,
      String entidade,
      Long entidadeId,
      Map<String, Object> antes,
      Map<String, Object> depois) {
    this.clienteId = clienteId;
    this.operacao = operacao;
    this.entidade = entidade;
    this.entidadeId = entidadeId;
    this.dadosAnteriores = antes;
    this.dadosNovos = depois;
    this.ator = "ADMIN_DEMO";
    this.ocorridaEm = OffsetDateTime.now();
    // Segredos nunca entram na trilha: troca de senha vira apenas um fato registrado.
    this.alteracoes =
        operacao.equals("ALTERAR_SENHA")
            ? "Senha alterada; conteúdo omitido."
            : operacao + " — " + entidade + " #" + entidadeId;
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
