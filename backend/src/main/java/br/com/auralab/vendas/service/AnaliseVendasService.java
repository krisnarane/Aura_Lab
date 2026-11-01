package br.com.auralab.vendas.service;

import br.com.auralab.clientes.service.RegraNegocioException;
import java.math.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF0055: histórico mensal de vendas por categoria, a partir do total histórico dos pedidos.
 *
 * <p>RN0074: entram no cálculo apenas as compras efetivadas e não devolvidas — APROVADA, EM
 * TRANSPORTE e ENTREGUE. Ficam fora EM_PROCESSAMENTO (pagamento ainda não validado), REPROVADA e os
 * estados de troca (EM_TROCA, TROCA_AUTORIZADA e TROCADO), em que a compra deixou de ser receita.
 */
@Service
public class AnaliseVendasService {
  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

  /** RN0074: fonte única da lista; o índice parcial da V13 repete exatamente estes valores. */
  static final List<String> STATUS_CONTABILIZAVEIS = List.of("APROVADA", "EM_TRANSPORTE", "ENTREGUE");

  // Interpolado como literal (e não como parâmetro) para o planejador poder usar o índice parcial.
  private static final String STATUS_EM_SQL =
      STATUS_CONTABILIZAVEIS.stream().map(s -> "'" + s + "'").collect(java.util.stream.Collectors.joining(","));

  private final JdbcTemplate jdbc;

  public AnaliseVendasService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public record Serie(String categoria, String descricao, List<BigDecimal> valores) {}
  public record Resultado(LocalDate inicio, LocalDate fim, List<String> meses, List<Serie> series) {}
  private record Linha(long pedidoId, OffsetDateTime criadoEm, BigDecimal total,
                       long itemId, BigDecimal subtotal, String categoria) {}
  private static final class Item {
    final long id;
    final BigDecimal subtotal;
    final SortedSet<String> categorias = new TreeSet<>();
    Item(long id, BigDecimal subtotal) { this.id = id; this.subtotal = subtotal; }
  }
  private static final class Pedido {
    final OffsetDateTime criadoEm;
    final BigDecimal total;
    final SortedMap<Long, Item> itens = new TreeMap<>();
    Pedido(OffsetDateTime criadoEm, BigDecimal total) { this.criadoEm = criadoEm; this.total = total; }
  }

  /** RN0072: o período é medido em meses cobertos, a mesma granularidade do agrupamento (RN0071). */
  static final int MINIMO_MESES = 1;

  static final int MAXIMO_MESES = 24;

  static int mesesCobertos(LocalDate inicio, LocalDate fim) {
    return (int) java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.from(inicio), YearMonth.from(fim)) + 1;
  }

  @Transactional(readOnly = true)
  public Resultado consultar(LocalDate inicio, LocalDate fim, List<String> categorias) {
    if (inicio == null || fim == null || fim.isBefore(inicio) || fim.isAfter(LocalDate.MAX.minusDays(1)))
      throw new RegraNegocioException("PERIODO_INVALIDO", "Informe um período válido, com início anterior ou igual ao fim.");
    // RN0072: o mínimo de um mês é consequência de fim >= inicio, pois meses parciais contam como um.
    int mesesDoPeriodo = mesesCobertos(inicio, fim);
    if (mesesDoPeriodo < MINIMO_MESES || mesesDoPeriodo > MAXIMO_MESES)
      throw new RegraNegocioException(
          "PERIODO_INVALIDO",
          "O período de análise deve cobrir de " + MINIMO_MESES + " a " + MAXIMO_MESES + " meses.");
    if (categorias == null || categorias.isEmpty() || categorias.stream().anyMatch(c -> c == null || c.isBlank()))
      throw new RegraNegocioException("CATEGORIAS_PRODUTO_INVALIDAS", "Selecione ao menos uma categoria.");
    var selecionadas = new LinkedHashSet<String>();
    for (String codigo : categorias) {
      if (!selecionadas.add(codigo))
        throw new RegraNegocioException("CATEGORIAS_PRODUTO_INVALIDAS", "Categorias repetidas não são permitidas.");
    }
    var descricoes = new HashMap<String, String>();
    jdbc.query("select codigo, descricao from categoria_produto", (org.springframework.jdbc.core.RowCallbackHandler) rs ->
      descricoes.put(rs.getString("codigo"), rs.getString("descricao")));
    if (!descricoes.keySet().containsAll(selecionadas))
      throw new RegraNegocioException("CATEGORIAS_PRODUTO_INVALIDAS", "Categoria inexistente.");

    var meses = new ArrayList<String>();
    var inicioMes = YearMonth.from(inicio);
    var fimMes = YearMonth.from(fim);
    for (var mes = inicioMes; !mes.isAfter(fimMes); mes = mes.plusMonths(1)) meses.add(mes.toString());
    var acumulado = new LinkedHashMap<String, long[]>();
    for (String codigo : selecionadas) acumulado.put(codigo, new long[meses.size()]);

    var limiteInferior = inicio.atStartOfDay(FUSO).toOffsetDateTime();
    var limiteSuperior = fim.plusDays(1).atStartOfDay(FUSO).toOffsetDateTime();
    final long[] pedidoAtualId = {-1};
    final Pedido[] pedidoAtual = {null};
    jdbc.query("""
      select p.id pedido_id, p.criado_em, p.total, i.id item_id, i.subtotal,
             string_agg(pc.categoria_codigo, ',' order by pc.categoria_codigo) categorias
        from pedido p
        join item_pedido i on i.pedido_id = p.id
        left join produto_categoria pc on pc.produto_id = i.produto_id
       where p.status in (%s) and p.criado_em >= ? and p.criado_em < ?
       group by p.id, i.id
       order by p.id, i.id
      """.formatted(STATUS_EM_SQL), rs -> {
        var linha = ler(rs);
        if (pedidoAtualId[0] != linha.pedidoId()) {
          if (pedidoAtual[0] != null) acumular(pedidoAtual[0], inicioMes, meses.size(), acumulado);
          pedidoAtualId[0] = linha.pedidoId();
          pedidoAtual[0] = new Pedido(linha.criadoEm(), linha.total());
        }
        var item = new Item(linha.itemId(), linha.subtotal());
        if (linha.categoria() != null) item.categorias.addAll(Arrays.asList(linha.categoria().split(",")));
        pedidoAtual[0].itens.put(item.id, item);
      }, limiteInferior, limiteSuperior);
    if (pedidoAtual[0] != null) acumular(pedidoAtual[0], inicioMes, meses.size(), acumulado);
    var series = new ArrayList<Serie>();
    for (String codigo : selecionadas) {
      var valores = Arrays.stream(acumulado.get(codigo)).mapToObj(c -> BigDecimal.valueOf(c, 2)).toList();
      series.add(new Serie(codigo, descricoes.get(codigo), valores));
    }
    return new Resultado(inicio, fim, meses, series);
  }

  private static void acumular(Pedido pedido, YearMonth inicioMes, int numeroMeses,
                              Map<String, long[]> acumulado) {
      int indice = (int) java.time.temporal.ChronoUnit.MONTHS.between(inicioMes,
          YearMonth.from(pedido.criadoEm.atZoneSameInstant(FUSO)));
      if (indice < 0 || indice >= numeroMeses) return;
      var itens = new ArrayList<>(pedido.itens.values());
      long totalCentavos = pedido.total.movePointRight(2).longValueExact();
      BigDecimal somaPesos = itens.stream().map(i -> i.subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
      long[] partes = new long[itens.size()];
      long distribuido = 0;
      for (int n = 0; n < itens.size(); n++) {
        BigDecimal peso = somaPesos.signum() == 0 ? BigDecimal.ONE : itens.get(n).subtotal;
        BigDecimal divisor = somaPesos.signum() == 0 ? BigDecimal.valueOf(itens.size()) : somaPesos;
        partes[n] = BigDecimal.valueOf(totalCentavos).multiply(peso)
            .divide(divisor, 0, RoundingMode.DOWN).longValueExact();
        distribuido += partes[n];
      }
      // Resíduo em ordem crescente de item; em cada item, de código de categoria.
      for (long resto = totalCentavos - distribuido, n = 0; n < resto; n++) partes[(int)(n % partes.length)]++;
      for (int n = 0; n < itens.size(); n++) {
        var codigos = itens.get(n).categorias;
        if (codigos.isEmpty()) continue;
        long base = partes[n] / codigos.size(), resto = partes[n] % codigos.size();
        for (String codigo : codigos) {
          if (acumulado.containsKey(codigo)) acumulado.get(codigo)[indice] += base + (resto > 0 ? 1 : 0);
          if (resto > 0) resto--;
        }
      }
  }

  private static Linha ler(ResultSet rs) throws SQLException {
    return new Linha(rs.getLong("pedido_id"), rs.getObject("criado_em", OffsetDateTime.class),
        rs.getBigDecimal("total"), rs.getLong("item_id"), rs.getBigDecimal("subtotal"),
        rs.getString("categorias"));
  }
}
