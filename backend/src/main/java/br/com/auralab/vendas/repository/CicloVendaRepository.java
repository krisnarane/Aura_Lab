package br.com.auralab.vendas.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistência das reservas e do pós-venda; o estoque físico continua em produto. */
@Repository
public class CicloVendaRepository {
  private final JdbcTemplate jdbc;

  public CicloVendaRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  // Ordem única das operações críticas no PostgreSQL, inclusive job, checkout e trocas.
  // Lock transacional compartilhado entre instâncias; não é um lock Java em memória.
  public void bloquearFluxo() {
    jdbc.execute("select pg_advisory_xact_lock(2026101205)");
  }

  public List<Long> clientesComReservas() {
    return jdbc.queryForList(
        "select cliente_id from reserva_estoque where estado='ATIVA' union select c.cliente_id from item_carrinho i join carrinho c on c.id=i.carrinho_id where not exists(select 1 from reserva_estoque r where r.item_carrinho_id=i.id) order by cliente_id",
        Long.class);
  }

  public List<Map<String,Object>> itensLegadosSemReserva(Long cliente) {
    return jdbc.queryForList("select i.*,p.nome,(select max(a.ocorrida_em) from auditoria_cliente a where a.cliente_id=c.cliente_id and a.entidade='CARRINHO' and a.entidade_id=c.id and a.operacao='ADICIONAR_ITEM_CARRINHO') as ultima_inclusao from item_carrinho i join carrinho c on c.id=i.carrinho_id join produto p on p.id=i.produto_id where c.cliente_id=? and not exists(select 1 from reserva_estoque r where r.item_carrinho_id=i.id) order by i.id",cliente);
  }

  public List<Map<String, Object>> reservas(Long cliente) {
    return jdbc.queryForList(
        "select r.*, p.nome, p.estoque, p.ativo, p.visivel, ip.pedido_id from reserva_estoque r join produto p on p.id=r.produto_id left join item_pedido ip on ip.id=r.item_pedido_id where r.cliente_id=? and r.estado='ATIVA' order by r.id",
        cliente);
  }

  public int disponivel(Long produto, Long itemProprio, OffsetDateTime agora) {
    return jdbc.queryForObject(
        "select greatest(0,p.estoque-coalesce((select sum(r.quantidade) from reserva_estoque r where r.produto_id=p.id and r.estado='ATIVA' and r.vence_em>? and (?::bigint is null or r.item_carrinho_id is distinct from ?)),0))::integer from produto p where p.id=?",
        Integer.class,
        agora,
        itemProprio,
        itemProprio,
        produto);
  }

  public Map<Long, Integer> reservasPorProduto(OffsetDateTime agora) {
    Map<Long, Integer> resultado = new HashMap<>();
    jdbc.query(
        "select produto_id,sum(quantidade)::integer as quantidade from reserva_estoque where estado='ATIVA' and vence_em>? group by produto_id",
        rs -> {
          resultado.put(rs.getLong("produto_id"), rs.getInt("quantidade"));
        },
        agora);
    return resultado;
  }

  public void reservar(
      Long cliente,
      Long carrinho,
      Long item,
      Long produto,
      int qtd,
      OffsetDateTime agora,
      OffsetDateTime fim) {
    jdbc.update(
        "insert into reserva_estoque(cliente_id,carrinho_id,item_carrinho_id,produto_id,quantidade,estado,iniciada_em,vence_em) values (?,?,?,?,?,'ATIVA',?,?) on conflict(item_carrinho_id) where estado='ATIVA' do update set quantidade=excluded.quantidade",
        cliente,
        carrinho,
        item,
        produto,
        qtd,
        agora,
        fim);
  }

  public void renovar(Long carrinho, OffsetDateTime fim) {
    jdbc.update(
        "update reserva_estoque set vence_em=? where carrinho_id=? and item_carrinho_id is not null and estado='ATIVA'",
        fim,
        carrinho);
  }

  public void liberarItem(Long item) {
    jdbc.update(
        "update reserva_estoque set estado='LIBERADA' where item_carrinho_id=? and estado='ATIVA'",
        item);
  }

  public void ajustarReserva(Long reserva, int quantidade) {
    jdbc.update("update reserva_estoque set quantidade=? where id=?", quantidade, reserva);
  }

  public void encerrarReserva(Long reserva, String estado) {
    jdbc.update("update reserva_estoque set estado=? where id=?", estado, reserva);
  }

  public void ajustarItem(Long item, int quantidade) {
    jdbc.update("update item_carrinho set quantidade=? where id=?", quantidade, item);
  }

  public void retirarItem(
      Long item,
      Long carrinho,
      Long produto,
      String nome,
      int quantidade,
      String motivo,
      long prazo,
      OffsetDateTime agora) {
    jdbc.update(
        "insert into item_retirado_carrinho(carrinho_id,produto_id,produto_nome,quantidade,motivo,prazo_minutos,retirado_em) values (?,?,?,?,?,?,?)",
        carrinho,
        produto,
        nome,
        quantidade,
        motivo,
        prazo,
        agora);
    jdbc.update("delete from item_carrinho where id=?", item);
  }

  public void marcarReadicionado(Long carrinho, Long produto) {
    jdbc.update(
        "update item_retirado_carrinho set readicionado=true where carrinho_id=? and produto_id=?",
        carrinho,
        produto);
  }

  public List<Map<String, Object>> retirados(Long carrinho) {
    return jdbc.queryForList(
        "select * from item_retirado_carrinho where carrinho_id=? order by id desc", carrinho);
  }

  public OffsetDateTime vencimentoCarrinho(Long carrinho) {
    Timestamp t =
        jdbc.queryForObject(
            "select min(vence_em) from reserva_estoque where carrinho_id=? and item_carrinho_id is not null and estado='ATIVA'",
            Timestamp.class,
            carrinho);
    return t == null ? null : t.toInstant().atOffset(ZoneOffset.UTC);
  }

  public void transferir(Long carrinho, Long produto, Long itemPedido) {
    int alterados =
        jdbc.update(
            "update reserva_estoque set item_carrinho_id=null,item_pedido_id=? where carrinho_id=? and produto_id=? and item_carrinho_id is not null and estado='ATIVA'",
            itemPedido,
            carrinho,
            produto);
    if (alterados != 1) throw new IllegalStateException("Reserva de checkout ausente");
  }

  public OffsetDateTime vencimentoPedido(Long pedido) {
    Timestamp t =
        jdbc.queryForObject(
            "select min(r.vence_em) from reserva_estoque r join item_pedido i on i.id=r.item_pedido_id where i.pedido_id=? and r.estado='ATIVA'",
            Timestamp.class,
            pedido);
    return t == null ? null : t.toInstant().atOffset(ZoneOffset.UTC);
  }

  public List<Map<String, Object>> reservasPedido(Long pedido) {
    return jdbc.queryForList(
        "select r.*,p.estoque,p.ativo,p.visivel from reserva_estoque r join item_pedido i on i.id=r.item_pedido_id join produto p on p.id=r.produto_id where i.pedido_id=? and r.estado='ATIVA' order by r.produto_id",
        pedido);
  }

  public void encerrarPedido(Long pedido, String estado) {
    jdbc.update(
        "update reserva_estoque r set estado=? from item_pedido i where i.id=r.item_pedido_id and i.pedido_id=? and r.estado='ATIVA'",
        estado,
        pedido);
  }

  public void reprovarExpirado(Long pedido) {
    jdbc.update(
        "update pedido set status='REPROVADA',pagamento_status='REPROVADO',motivo_resultado='RESERVA_EXPIRADA' where id=? and status='EM_PROCESSAMENTO' and not legado",
        pedido);
    jdbc.update(
        "update cupom set situacao='ATIVO',pedido_reserva_id=null where pedido_reserva_id=? and situacao='RESERVADO'",
        pedido);
  }

  public Long notificar(
      Long cliente, String chave, String tipo, String mensagem, OffsetDateTime agora) {
    var rows =
        jdbc.queryForList(
            "insert into notificacao_venda(cliente_id,chave,tipo,mensagem,criada_em) values (?,?,?,?,?) on conflict(chave) do nothing returning id",
            cliente,
            chave,
            tipo,
            mensagem,
            agora);
    return rows.isEmpty() ? null : ((Number) rows.getFirst().get("id")).longValue();
  }

  public Map<String, Object> pedido(Long id) {
    return jdbc.queryForMap("select * from pedido where id=?", id);
  }

  public void tocarCarrinho(Long id, OffsetDateTime agora) {
    jdbc.update("update carrinho set atualizado_em=? where id=?", agora, id);
  }

  public List<Map<String, Object>> notificacoes(Long cliente) {
    return jdbc.queryForList(
        "select id,tipo,mensagem,criada_em,lida from notificacao_venda where cliente_id=? order by criada_em desc,id desc limit 100",
        cliente);
  }

  /** Avisos da reserva vigente; notificações históricas continuam no perfil do cliente. */
  public List<Map<String, Object>> avisosCarrinho(Long carrinho, OffsetDateTime agora) {
    return jdbc.queryForList(
        """
        select n.id,n.tipo,n.mensagem,n.criada_em,n.lida
        from notificacao_venda n join carrinho c on c.cliente_id=n.cliente_id
        where c.id=? and (
          (n.tipo='RESERVA_VENCENDO' and exists (
            select 1 from reserva_estoque r
            where r.carrinho_id=c.id and r.item_carrinho_id is not null and r.estado='ATIVA'
              and r.vence_em>? and r.vence_em<=? + interval '5 minutes'
              and n.chave like 'aviso-' || r.id || '-%'
              and n.criada_em>=r.vence_em-interval '5 minutes'
          )) or (n.tipo='ESTOQUE_ALTERADO' and n.criada_em>=c.atualizado_em)
        ) order by n.criada_em desc,n.id desc
        """,
        carrinho, agora, agora);
  }

  public void lerNotificacao(Long cliente, Long id) {
    jdbc.update("update notificacao_venda set lida=true where cliente_id=? and id=?", cliente, id);
  }

  public void baixar(Long produto, int quantidade, Long item, OffsetDateTime agora) {
    int rows =
        jdbc.update(
            "update produto set estoque=estoque-? where id=? and estoque>=?",
            quantidade,
            produto,
            quantidade);
    if (rows != 1) throw new IllegalStateException("Estoque insuficiente na baixa atômica");
    jdbc.update(
        "insert into movimentacao_estoque(produto_id,tipo,quantidade,item_pedido_id,ocorrida_em) values (?,'BAIXA',?,?,?)",
        produto,
        quantidade,
        item,
        agora);
  }

  public void reentrar(Long produto, int quantidade, Long itemTroca, OffsetDateTime agora) {
    jdbc.update(
        "insert into movimentacao_estoque(produto_id,tipo,quantidade,item_troca_id,ocorrida_em) values (?,'REENTRADA',?,?,?)",
        produto,
        quantidade,
        itemTroca,
        agora);
    jdbc.update("update produto set estoque=estoque+? where id=?", quantidade, produto);
  }

  public void resultadoParcela(Long parcela, String resultado) {
    jdbc.update("update pagamento_parte set resultado_validacao=? where id=?", resultado, parcela);
  }

  public List<Map<String, Object>> pedidoIds(String codigo, Long cliente, String status) {
    return jdbc.queryForList(
        "select p.id,p.cliente_id from pedido p where (?::text is null or p.codigo=?) and (?::bigint is null or p.cliente_id=?) and (?::text is null or p.status=?) order by p.criado_em desc",
        codigo,
        codigo,
        cliente,
        cliente,
        status,
        status);
  }

  public List<Map<String, Object>> historico(Long pedido) {
    return jdbc.queryForList(
        "select ocorrida_em,operacao,ator,dados_novos->>'status' as status from auditoria_cliente where entidade='PEDIDO' and entidade_id=? order by ocorrida_em,id",
        pedido);
  }

  public record Metadados(
      Map<Long, Integer> comprometidas,
      Map<Long, OffsetDateTime> vencimentos,
      Map<Long, List<Map<String, Object>>> historicos) {}

  public Metadados metadados(Collection<Long> pedidos) {
    Map<Long, Integer> quantidades = new HashMap<>();
    Map<Long, OffsetDateTime> prazos = new HashMap<>();
    Map<Long, List<Map<String, Object>>> historicos = new HashMap<>();
    if (pedidos.isEmpty()) return new Metadados(quantidades, prazos, historicos);
    String ids =
        "{"
            + pedidos.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","))
            + "}";
    for (var r :
        jdbc.queryForList(
            "select it.item_pedido_id, sum(it.quantidade)::integer as quantidade from item_troca it join item_pedido i on i.id=it.item_pedido_id where i.pedido_id=any(?::bigint[]) group by it.item_pedido_id",
            ids)) quantidades.put(id(r, "item_pedido_id"), qtd(r, "quantidade"));
    for (var r :
        jdbc.queryForList(
            "select i.pedido_id,min(r.vence_em) as vence_em from reserva_estoque r join item_pedido i on i.id=r.item_pedido_id where r.estado='ATIVA' and i.pedido_id=any(?::bigint[]) group by i.pedido_id",
            ids)) prazos.put(id(r, "pedido_id"), tempo(r, "vence_em"));
    for (var r :
        jdbc.queryForList(
            "select entidade_id,ocorrida_em,operacao,ator,dados_novos->>'status' as status from auditoria_cliente where entidade='PEDIDO' and entidade_id=any(?::bigint[]) order by ocorrida_em,id",
            ids)) historicos.computeIfAbsent(id(r, "entidade_id"), k -> new ArrayList<>()).add(r);
    return new Metadados(quantidades, prazos, historicos);
  }

  public Long checkoutExistente(Long cliente, String chave, String resumo) {
    var rows =
        jdbc.queryForList(
            "select id,resumo_checkout from pedido where cliente_id=? and chave_checkout=?",
            cliente,
            chave);
    if (rows.isEmpty()) return null;
    if (!Objects.equals(rows.getFirst().get("resumo_checkout"), resumo))
      throw new br.com.auralab.clientes.service.RegraNegocioException(
          "IDEMPOTENCIA_CONFLITO", "Chave reutilizada com outra compra.");
    return ((Number) rows.getFirst().get("id")).longValue();
  }

  public Long criarTroca(
      Long pedido,
      Long cliente,
      boolean integral,
      String motivo,
      OffsetDateTime agora,
      String chave,
      String resumo) {
    return jdbc.queryForObject(
        "insert into troca(pedido_id,cliente_id,integral,status,motivo,criada_em,chave_solicitacao,resumo_solicitacao) values (?,?,?,'EM_TROCA',?,?,?,?) returning id",
        Long.class,
        pedido,
        cliente,
        integral,
        motivo,
        agora,
        chave,
        resumo);
  }

  public Long trocaExistente(Long cliente, String chave, String resumo) {
    var rows =
        jdbc.queryForList(
            "select id,resumo_solicitacao from troca where cliente_id=? and chave_solicitacao=?",
            cliente,
            chave);
    if (rows.isEmpty()) return null;
    if (!Objects.equals(rows.getFirst().get("resumo_solicitacao"), resumo))
      throw new br.com.auralab.clientes.service.RegraNegocioException(
          "IDEMPOTENCIA_CONFLITO", "Chave reutilizada com outra troca.");
    return ((Number) rows.getFirst().get("id")).longValue();
  }

  public void adicionarItemTroca(Long troca, Long item, int qtd, BigDecimal valor) {
    jdbc.update(
        "insert into item_troca(troca_id,item_pedido_id,quantidade,valor_liquido) values (?,?,?,?)",
        troca,
        item,
        qtd,
        valor);
  }

  public int comprometidas(Long item) {
    return jdbc.queryForObject(
        "select coalesce(sum(quantidade),0)::integer from item_troca where item_pedido_id=?",
        Integer.class,
        item);
  }

  public Map<String, Object> troca(Long id) {
    var rows = jdbc.queryForList("select * from troca where id=?", id);
    if (rows.isEmpty())
      throw new br.com.auralab.clientes.service.RegraNegocioException(
          "TROCA_NAO_ENCONTRADA", "Troca não encontrada.");
    return rows.getFirst();
  }

  public List<Map<String, Object>> trocas(Long cliente, Long pedido) {
    return jdbc.queryForList(
        "select * from troca where (?::bigint is null or cliente_id=?) and (?::bigint is null or pedido_id=?) order by id desc",
        cliente,
        cliente,
        pedido,
        pedido);
  }

  public List<Map<String, Object>> itensTroca(Long troca) {
    return jdbc.queryForList(
        "select it.*,i.produto_id,i.produto_nome from item_troca it join item_pedido i on i.id=it.item_pedido_id where it.troca_id=? order by it.id",
        troca);
  }

  public void autorizar(Long troca, OffsetDateTime agora) {
    jdbc.update(
        "update troca set status='TROCA_AUTORIZADA',autorizada_em=? where id=?", agora, troca);
  }

  public void receber(Long troca, boolean retornar, Long cupom, OffsetDateTime agora) {
    jdbc.update(
        "update troca set status='TROCADO',recebida_em=?,retornar_estoque=?,cupom_id=? where id=?",
        agora,
        retornar,
        cupom,
        troca);
  }

  public void transacao(
      Long cliente,
      String entidade,
      Long origem,
      String codigo,
      String tipo,
      String status,
      BigDecimal valor,
      String detalhes,
      OffsetDateTime agora) {
    jdbc.update(
        "insert into transacao_cliente(cliente_id,codigo,tipo,status,valor,ocorrida_em,detalhes,origem,origem_entidade,origem_id) values (?,?,?,?,?,?,?,'REAL',?,?) on conflict(origem_entidade,origem_id) where origem_id is not null do update set status=excluded.status,valor=excluded.valor,detalhes=excluded.detalhes",
        cliente,
        codigo,
        tipo,
        status,
        valor,
        agora,
        detalhes,
        entidade,
        origem);
  }

  public static Long id(Map<String, Object> row, String campo) {
    return row.get(campo) == null ? null : ((Number) row.get(campo)).longValue();
  }

  public static int qtd(Map<String, Object> row, String campo) {
    return ((Number) row.get(campo)).intValue();
  }

  public static OffsetDateTime tempo(Map<String, Object> row, String campo) {
    Object v = row.get(campo);
    return v == null
        ? null
        : v instanceof OffsetDateTime t ? t : ((Timestamp) v).toInstant().atOffset(ZoneOffset.UTC);
  }
}
