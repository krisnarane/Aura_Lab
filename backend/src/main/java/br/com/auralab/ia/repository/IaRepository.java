package br.com.auralab.ia.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consultas SQL do incremento RNF0044, sem dados pessoais no contexto da IA. */
@Repository
public class IaRepository {
  public record Modelo(String versao, String endpoint) {}
  public record Produto(Long id, String nome, String descricao, String categorias,
      BigDecimal preco, String imagemUrl) {}
  public record Mensagem(Long id, String papel, String texto, List<Long> produtosIds,
      String modeloVersao, OffsetDateTime criadaEm, Boolean util, String motivo) {}
  public record Conversa(Long id, OffsetDateTime criadaEm) {}

  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  public IaRepository(JdbcTemplate jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public boolean clienteAtivo(Long id) {
    return Boolean.TRUE.equals(jdbc.queryForObject(
        "select exists(select 1 from cliente where id=? and ativo)", Boolean.class, id));
  }

  public boolean clienteExiste(Long id) {
    return Boolean.TRUE.equals(jdbc.queryForObject(
        "select exists(select 1 from cliente where id=?)", Boolean.class, id));
  }

  public List<String> preferencias(Long cliente) {
    return jdbc.queryForList("select categoria_codigo from cliente_preferencia where cliente_id=? order by categoria_codigo",
        String.class, cliente);
  }

  public boolean categoriasValidas(List<String> codigos) {
    if (codigos == null || codigos.isEmpty()) return false;
    String marcadores = String.join(",", Collections.nCopies(codigos.size(), "?"));
    Integer n = jdbc.queryForObject("select count(*) from categoria_produto where codigo in (" + marcadores + ")",
        Integer.class, codigos.toArray());
    return n != null && n == codigos.size();
  }

  public void substituirPreferencias(Long cliente, List<String> codigos) {
    jdbc.update("delete from cliente_preferencia where cliente_id=?", cliente);
    for (String codigo : codigos)
      jdbc.update("insert into cliente_preferencia(cliente_id,categoria_codigo) values (?,?)", cliente, codigo);
  }

  public Optional<Modelo> modeloAtivo() {
    return jdbc.query("select versao, endpoint from ia_modelo where ativo", (r, i) ->
        new Modelo(r.getString(1), r.getString(2))).stream().findFirst();
  }

  public List<String> compras(Long cliente) {
    return jdbc.query("select i.produto_nome from item_pedido i join pedido p on p.id=i.pedido_id "
        + "where p.cliente_id=? and p.pagamento_status='APROVADO' and not p.legado "
        + "order by p.criado_em desc, i.id desc limit 12", (r, i) -> r.getString(1), cliente);
  }

  public List<Produto> candidatos(Long cliente) {
    return jdbc.query("select p.id,p.nome,p.descricao,coalesce(string_agg(distinct pc.categoria_codigo, ',') ,'') categorias,p.preco,p.imagem_url "
        + "from produto p left join produto_categoria pc on pc.produto_id=p.id "
        + "where p.ativo and p.visivel and p.estoque>0 group by p.id "
        + "order by count(pc.categoria_codigo) filter (where pc.categoria_codigo in "
        + "(select categoria_codigo from cliente_preferencia where cliente_id=?)) desc,p.id limit 12",
        (r, i) -> new Produto(r.getLong(1), r.getString(2), r.getString(3), r.getString(4),
            r.getBigDecimal(5), r.getString(6)), cliente);
  }

  public boolean conversaDoCliente(Long conversa, Long cliente) {
    return Boolean.TRUE.equals(jdbc.queryForObject(
        "select exists(select 1 from ia_conversa where id=? and cliente_id=?)", Boolean.class, conversa, cliente));
  }

  public List<Conversa> conversas(Long cliente) {
    return jdbc.query("select id,criada_em from ia_conversa where cliente_id=? order by criada_em desc,id desc",
        (r, i) -> new Conversa(r.getLong(1), r.getObject(2, OffsetDateTime.class)), cliente);
  }

  public List<Mensagem> mensagens(Long conversa) {
    return jdbc.query("select m.id,m.papel,m.texto,m.produtos_ids::text,m.modelo_versao,m.criada_em,a.util,a.motivo "
        + "from ia_mensagem m left join ia_avaliacao a on a.resposta_id=m.id "
        + "where m.conversa_id=? order by m.id", (r, i) -> {
          List<Long> ids;
          try { ids = json.readValue(r.getString(4), new TypeReference<List<Long>>() {}); }
          catch (Exception ex) { throw new IllegalStateException("Produtos inválidos na mensagem", ex); }
          return new Mensagem(r.getLong(1), r.getString(2), r.getString(3), ids,
              r.getString(5), r.getObject(6, OffsetDateTime.class), (Boolean) r.getObject(7), r.getString(8));
        }, conversa);
  }

  public Long criarConversa(Long cliente) {
    return jdbc.queryForObject("insert into ia_conversa(cliente_id) values (?) returning id", Long.class, cliente);
  }

  public Long criarMensagem(Long conversa, String papel, String texto, List<Long> produtos, String versao) {
    try {
      return jdbc.queryForObject("insert into ia_mensagem(conversa_id,papel,texto,produtos_ids,modelo_versao) "
          + "values (?,?,?,?::jsonb,?) returning id", Long.class,
          conversa, papel, texto, json.writeValueAsString(produtos), versao);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public boolean respostaDoCliente(Long resposta, Long cliente) {
    return Boolean.TRUE.equals(jdbc.queryForObject(
        "select exists(select 1 from ia_mensagem m join ia_conversa c on c.id=m.conversa_id "
        + "where m.id=? and m.papel='ASSISTANT' and c.cliente_id=?)", Boolean.class, resposta, cliente));
  }

  public void avaliar(Long resposta, boolean util, String motivo) {
    jdbc.update("insert into ia_avaliacao(resposta_id,util,motivo) values (?,?,?) "
        + "on conflict(resposta_id) do update set util=excluded.util,motivo=excluded.motivo,"
        + "correcao=null,atualizada_em=current_timestamp", resposta, util, motivo);
  }

  public Optional<Mensagem> resposta(Long id) {
    return jdbc.query("select conversa_id from ia_mensagem where id=?", (r, i) -> r.getLong(1), id)
        .stream().findFirst().flatMap(conversa -> mensagens(conversa).stream().filter(m -> m.id().equals(id)).findFirst());
  }
}
