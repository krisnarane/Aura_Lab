package br.com.auralab.vendas;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.auralab.PostgresTest;
import br.com.auralab.vendas.service.ReservaEstoqueService;
import br.com.auralab.vendas.service.VendaService;
import br.com.auralab.vendas.service.CicloPedidoService;
import br.com.auralab.vendas.api.dto.CheckoutInput;
import br.com.auralab.vendas.api.dto.ParcelaCartaoInput;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(CicloVendaApiTest.Tempo.class)
class CicloVendaApiTest extends PostgresTest {
  @TestConfiguration
  static class Tempo {
    @Bean
    @Primary
    Relogio relogio() {
      return new Relogio();
    }
  }

  static class Relogio extends Clock {
    volatile Instant instante = Instant.parse("2026-10-12T12:00:00Z");

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId z) {
      return this;
    }

    public Instant instant() {
      return instante;
    }

    void avancar(Duration d) {
      instante = instante.plus(d);
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired Relogio clock;
  @Autowired ReservaEstoqueService reservas;
  @Autowired VendaService vendas;
  @Autowired CicloPedidoService cicloPedido;

  @BeforeEach
  void horario() {
    clock.instante = Instant.parse("2026-10-12T12:00:00Z");
  }

  JsonNode postJson(String url, Object dados, int esperado) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/v1" + url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(dados)))
            .andExpect(status().is(esperado))
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  JsonNode adicionar(int qty) throws Exception {
    return postJson("/clientes/1/carrinho/itens", Map.of("produtoId", 1, "quantidade", qty), 201);
  }

  @Test
  void RN0044_RN0045_RNF0042_reinclusaoNaoMostraAvisoDeReservaAnterior() throws Exception {
    adicionar(1);
    clock.avancar(Duration.ofMinutes(15));
    reservas.processarVencimentos();
    mvc.perform(get("/api/v1/clientes/1/carrinho"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.avisos.length()").value(1));
    clock.avancar(Duration.ofMinutes(5));
    reservas.processarVencimentos();
    JsonNode novo = adicionar(1);
    assertThat(novo.path("avisos").size()).isZero();
    assertThat(novo.path("compraPermitida").asBoolean()).isTrue();
    assertThat(novo.path("retirados").get(0).path("readicionado").asBoolean()).isTrue();
    assertThat(contar("notificacao_venda")).isEqualTo(2);
  }

  @Test
  void RN0044_renovacaoOcultaAvisoDoVencimentoAnterior() throws Exception {
    adicionar(1);
    clock.avancar(Duration.ofMinutes(15));
    reservas.processarVencimentos();
    assertThat(adicionar(1).path("avisos").size()).isZero();
    assertThat(contar("notificacao_venda")).isEqualTo(1);
    clock.avancar(Duration.ofMinutes(15));
    reservas.processarVencimentos();
    mvc.perform(get("/api/v1/clientes/1/carrinho"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.avisos.length()").value(1));
  }

  long pedido(int qty) throws Exception {
    adicionar(qty);
    long cartao =
        postJson(
                "/clientes/1/cartoes",
                Map.of(
                    "numero",
                    "4111111111111111",
                    "titular",
                    "MARINA COSTA",
                    "bandeiraId",
                    1,
                    "codigoSeguranca",
                    "123",
                    "preferencial",
                    false),
                201)
            .path("id")
            .asLong();
    var total =
        postJson("/clientes/1/carrinho/resumo", Map.of("enderecoId", 1), 200)
            .path("total")
            .decimalValue();
    return vendas.finalizar(1L, new CheckoutInput(1L, null, false, null, List.of(),
        List.of(new ParcelaCartaoInput(cartao, null, false, total)))).id();
  }

  JsonNode validar(long p, String resultado) throws Exception {
    jdbc.update("insert into resultado_operadora_cartao(cartao_id,resultado) "
        + "select cartao_id, ? from pagamento_parte where pedido_id=? and cartao_id is not null "
        + "on conflict (cartao_id) do update set resultado=excluded.resultado", resultado, p);
    return json.valueToTree(cicloPedido.validar(p));
  }

  void entregar(long p) throws Exception {
    validar(p, "APROVADO");
    postJson("/admin/pedidos/" + p + "/despacho", Map.of(), 200);
    postJson("/admin/pedidos/" + p + "/entrega", Map.of(), 200);
  }

  int contar(String tabela) {
    return jdbc.queryForObject("select count(*) from " + tabela, Integer.class);
  }

  int estoque() {
    return jdbc.queryForObject("select estoque from produto where id=1", Integer.class);
  }

  @Test
  void RN0037_RN0038_RN0028_checkoutAutomaticoReprovaSeUmCartaoFalhar() throws Exception {
    adicionar(1);
    long aprovado = jdbc.queryForObject(
        "insert into cartao(cliente_id,bandeira_id,titular,ultimos_quatro,preferencial,ativo) "
            + "values(1,1,'MARINA COSTA','1111',false,true) returning id", Long.class);
    long reprovado = jdbc.queryForObject(
        "insert into cartao(cliente_id,bandeira_id,titular,ultimos_quatro,preferencial,ativo) "
            + "values(1,2,'MARINA COSTA','2222',false,true) returning id", Long.class);
    jdbc.update("insert into resultado_operadora_cartao(cartao_id,resultado) values(?,'REPROVADO')", reprovado);
    long cupom = jdbc.queryForObject(
        "insert into cupom(tipo,cliente_id,valor,situacao,criado_em) "
            + "values('TROCA',1,30.00,'ATIVO',current_timestamp) returning id", Long.class);
    JsonNode resultado = postJson("/clientes/1/pedidos", Map.of("enderecoId", 1,
        "cuponsTroca", List.of(cupom),
        "cartoes", List.of(Map.of("cartaoId", aprovado, "valor", "30.00"),
            Map.of("cartaoId", reprovado, "valor", "46.90"))), 201);
    assertThat(resultado.path("status").asText()).isEqualTo("REPROVADA");
    assertThat(resultado.path("historico").toString()).contains("EM_PROCESSAMENTO", "REPROVADA");
    assertThat(estoque()).isEqualTo(25);
    assertThat(contar("movimentacao_estoque")).isZero();
    assertThat(jdbc.queryForObject("select situacao from cupom where id=?", String.class, cupom))
        .isEqualTo("ATIVO");
    assertThat(jdbc.queryForObject("select count(*) from reserva_estoque where estado='ATIVA'", Integer.class)).isZero();
    postJson("/admin/pedidos/" + resultado.path("id").asLong() + "/despacho", Map.of(), 400);
  }

  @Test
  void RNF0012_falhaNaValidacaoAutomaticaReverteCheckoutCompleto() throws Exception {
    adicionar(1);
    long cartao = jdbc.queryForObject(
        "insert into cartao(cliente_id,bandeira_id,titular,ultimos_quatro,preferencial,ativo) "
            + "values(1,1,'MARINA COSTA','1111',false,true) returning id", Long.class);
    jdbc.execute("create function falhar_checkout_teste() returns trigger language plpgsql as $$ "
        + "begin if NEW.operacao='VALIDAR_PAGAMENTO' then raise exception 'Falha induzida'; "
        + "end if; return NEW; end $$");
    jdbc.execute("create trigger falhar_checkout_teste before insert on auditoria_cliente "
        + "for each row execute function falhar_checkout_teste()");
    try {
      assertThatThrownBy(() -> postJson("/clientes/1/pedidos",
          Map.of("enderecoId", 1, "cartoes", List.of(Map.of("cartaoId", cartao, "valor", "106.90"))),
          201)).isInstanceOf(Exception.class);
      assertThat(contar("pedido")).isZero();
      assertThat(contar("item_carrinho")).isEqualTo(1);
      assertThat(estoque()).isEqualTo(25);
      assertThat(contar("movimentacao_estoque")).isZero();
    } finally {
      jdbc.execute("drop trigger falhar_checkout_teste on auditoria_cliente");
      jdbc.execute("drop function falhar_checkout_teste()");
    }
  }

  @Test
  void
      RF0038_RF0039_RF0040_RF0053_RN0028_RN0037_RN0038_RN0039_RN0040_baixaSomenteAposTodasAsFormasValidas()
          throws Exception {
    long p = pedido(2);
    assertThat(estoque()).isEqualTo(25);
    assertThat(validar(p, "APROVADO").path("status").asText()).isEqualTo("APROVADA");
    assertThat(estoque()).isEqualTo(23);
    validar(p, "APROVADO");
    assertThat(contar("movimentacao_estoque")).isEqualTo(1);
    postJson("/admin/pedidos/" + p + "/despacho", Map.of(), 200);
    postJson("/admin/pedidos/" + p + "/entrega", Map.of(), 200);
    assertThat(jdbc.queryForObject("select status from pedido where id=?", String.class, p))
        .isEqualTo("ENTREGUE");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from transacao_cliente where origem='REAL' and tipo='PEDIDO'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void RN0038_reprovacaoLiberaReservaSemBaixaFisica() throws Exception {
    long p = pedido(1);
    assertThat(validar(p, "REPROVADO").path("status").asText()).isEqualTo("REPROVADA");
    assertThat(estoque()).isEqualTo(25);
    assertThat(contar("movimentacao_estoque")).isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from reserva_estoque where estado='ATIVA'", Integer.class))
        .isZero();
    assertThat(validar(p, "APROVADO").path("status").asText()).isEqualTo("REPROVADA");
  }

  @Test
  void RN0044_RN0045_RNF0042_avisaExpiraERetiraSemEsperarMinutosReais() throws Exception {
    adicionar(1);
    clock.avancar(Duration.ofMinutes(15));
    reservas.processarVencimentos();
    reservas.processarVencimentos();
    assertThat(contar("notificacao_venda")).isEqualTo(1);
    clock.avancar(Duration.ofMinutes(5));
    reservas.processarVencimentos();
    assertThat(contar("item_carrinho")).isZero();
    assertThat(contar("item_retirado_carrinho")).isEqualTo(1);
    assertThat(contar("notificacao_venda")).isEqualTo(2);
    adicionar(1);
    assertThat(
            jdbc.queryForObject("select readicionado from item_retirado_carrinho", Boolean.class))
        .isTrue();
  }

  @Test
  void RN0045_expiracaoDoPedidoImpedeAprovacaoTardia() throws Exception {
    long p = pedido(1);
    clock.avancar(Duration.ofMinutes(20));
    reservas.processarVencimentos();
    assertThat(validar(p, "APROVADO").path("motivoResultado").asText())
        .isEqualTo("RESERVA_EXPIRADA");
    assertThat(estoque()).isEqualTo(25);
  }

  @Test
  void RN0032_CT039_ajustaERemoveAutomaticamenteQuandoEstoqueMuda() throws Exception {
    adicionar(3);
    jdbc.update("update produto set estoque=2 where id=1");
    reservas.processarVencimentos();
    assertThat(jdbc.queryForObject("select quantidade from item_carrinho", Integer.class))
        .isEqualTo(2);
    jdbc.update("update produto set estoque=0 where id=1");
    reservas.processarVencimentos();
    assertThat(contar("item_carrinho")).isZero();
    assertThat(contar("notificacao_venda")).isEqualTo(2);
  }

  @Test
  void RN0045_ultimaInclusaoRenovaMasAlteracaoNaoRenovaPrazo() throws Exception {
    long item = adicionar(1).path("itens").get(0).path("id").asLong();
    clock.avancar(Duration.ofMinutes(10));
    mvc.perform(
            put("/api/v1/clientes/1/carrinho/itens/" + item)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantidade\":2}"))
        .andExpect(status().isOk());
    clock.avancar(Duration.ofMinutes(9));
    adicionar(1);
    clock.avancar(Duration.ofMinutes(2));
    reservas.processarVencimentos();
    assertThat(contar("item_carrinho")).isEqualTo(1);
    clock.avancar(Duration.ofMinutes(18));
    reservas.processarVencimentos();
    assertThat(contar("item_carrinho")).isZero();
  }

  @Test
  void
      RF0041_RF0043_RF0044_RF0045_RF0054_RN0041_RN0042_RN0043_RN0046_trocaParcialRecebeGeraCupomEReentraUmaVez()
          throws Exception {
    long p = pedido(2);
    entregar(p);
    long item = jdbc.queryForObject("select id from item_pedido where pedido_id=?", Long.class, p);
    long t =
        postJson(
                "/clientes/1/pedidos/" + p + "/trocas",
                Map.of(
                    "integral",
                    false,
                    "itemPedidoId",
                    item,
                    "quantidade",
                    1,
                    "motivo",
                    "Avaria fictícia"),
                201)
            .path("id")
            .asLong();
    assertThat(jdbc.queryForObject("select status from pedido", String.class))
        .isEqualTo("ENTREGUE");
    postJson("/admin/trocas/" + t + "/autorizacao", Map.of(), 200);
    postJson("/admin/trocas/"+t+"/autorizacao",Map.of(),200);
    assertThat(jdbc.queryForObject("select count(*) from notificacao_venda where tipo='TROCA_AUTORIZADA'",Integer.class)).isEqualTo(1);
    var recebida =
        postJson("/admin/trocas/" + t + "/recebimento", Map.of(), 200);
    assertThat(recebida.path("cupom").path("valor").decimalValue()).isEqualByComparingTo("89.90");
    assertThat(estoque()).isEqualTo(24);
    postJson("/admin/trocas/" + t + "/recebimento", Map.of(), 200);
    assertThat(contar("cupom")).isEqualTo(1);
    assertThat(contar("movimentacao_estoque")).isEqualTo(2);
  }

  @Test
  void RF0042_RF0054_trocaIntegralAcompanhaCompraEReentraNoEstoque() throws Exception {
    long p = pedido(1);
    entregar(p);
    long t =
        postJson(
                "/clientes/1/pedidos/" + p + "/trocas",
                Map.of("integral", true, "motivo", "Troca integral fictícia"),
                201)
            .path("id")
            .asLong();
    assertThat(jdbc.queryForObject("select status from pedido", String.class))
        .isEqualTo("EM_TROCA");
    postJson("/admin/trocas/" + t + "/autorizacao", Map.of(), 200);
    assertThat(jdbc.queryForObject("select status from pedido", String.class))
        .isEqualTo("TROCA_AUTORIZADA");
    postJson("/admin/trocas/" + t + "/recebimento", Map.of(), 200);
    assertThat(estoque()).isEqualTo(25);
    assertThat(jdbc.queryForObject("select status from pedido", String.class)).isEqualTo("TROCADO");
  }

  @Test
  void RF0041_RN0041_RN0043_recusaTrocaAntesDaEntrega() throws Exception {
    long p = pedido(1);
    postJson(
        "/clientes/1/pedidos/" + p + "/trocas",
        Map.of("integral", true, "motivo", "Inválida"),
        400);
    assertThat(contar("troca")).isZero();
  }

  @Test
  void RN0031_RN0044_concorrenciaPelaUltimaUnidadeNaoVendeDuasVezes() throws Exception {
    jdbc.update("update produto set estoque=1 where id=1");
    jdbc.update("update cliente set ativo=true where id=2");
    var inicio = new java.util.concurrent.CountDownLatch(1);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var tarefas =
          List.of(1L, 2L).stream()
              .map(
                  c ->
                      pool.submit(
                          () -> {
                            inicio.await();
                            return mvc.perform(
                                    post("/api/v1/clientes/" + c + "/carrinho/itens")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"produtoId\":1,\"quantidade\":1}"))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                          }))
              .toList();
      inicio.countDown();
      assertThat(List.of(tarefas.get(0).get(), tarefas.get(1).get()))
          .containsExactlyInAnyOrder(201, 409);
    }
    assertThat(
            jdbc.queryForObject(
                "select sum(quantidade) from reserva_estoque where estado='ATIVA'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void RN0037_todasParcelasPrecisamSerAprovadas() throws Exception {
    long p = pedido(1);
    assertThat(validar(p, "FALHA").path("status").asText()).isEqualTo("REPROVADA");
    assertThat(estoque()).isEqualTo(25);
  }

  @Test
  void RF0041_RN0041_quantidadeJaComprometidaNaoPodeGerarCreditoDuplicado() throws Exception {
    long p = pedido(1);
    entregar(p);
    long item = jdbc.queryForObject("select id from item_pedido", Long.class);
    var dados =
        Map.of(
            "integral", false, "itemPedidoId", item, "quantidade", 1, "motivo", "Teste fictício");
    postJson("/clientes/1/pedidos/" + p + "/trocas", dados, 201);
    postJson("/clientes/1/pedidos/" + p + "/trocas", dados, 400);
    assertThat(contar("troca")).isEqualTo(1);
  }

  @Test
  void RF0044_recebimentoExigeAutorizacao() throws Exception {
    long p = pedido(1);
    entregar(p);
    long t =
        postJson(
                "/clientes/1/pedidos/" + p + "/trocas",
                Map.of("integral", true, "motivo", "Teste fictício"),
                201)
            .path("id")
            .asLong();
    postJson("/admin/trocas/" + t + "/recebimento", Map.of(), 400);
    assertThat(contar("cupom")).isZero();
    assertThat(estoque()).isEqualTo(24);
  }

  @Test
  void RNF0012_falhaDeAuditoriaRevertePagamentoEBaixaNaMesmaTransacao() throws Exception {
    long p = pedido(1);
    int antes = contar("auditoria_cliente");
    jdbc.execute(
        "create function falhar_auditoria_teste() returns trigger language plpgsql as $$ begin if NEW.operacao='VALIDAR_PAGAMENTO' then raise exception 'Falha de auditoria induzida pelo teste'; end if; return NEW; end $$");
    jdbc.execute(
        "create trigger falha_auditoria_teste before insert on auditoria_cliente for each row execute function falhar_auditoria_teste()");
    try {
      assertThatThrownBy(() -> validar(p, "APROVADO"))
          .isInstanceOf(RuntimeException.class);
      assertThat(estoque()).isEqualTo(25);
      assertThat(contar("movimentacao_estoque")).isZero();
      assertThat(contar("auditoria_cliente")).isEqualTo(antes);
      assertThat(jdbc.queryForObject("select status from pedido where id=?", String.class, p))
          .isEqualTo("EM_PROCESSAMENTO");
      assertThat(jdbc.queryForObject("select estado from reserva_estoque", String.class))
          .isEqualTo("ATIVA");
    } finally {
      jdbc.execute("drop trigger falha_auditoria_teste on auditoria_cliente");
      jdbc.execute("drop function falhar_auditoria_teste()");
    }
  }

  @Test
  void RF0025_RN0027_historicoRealNaoContaPagamentoECupomComoNovaCompra() throws Exception {
    long p = pedido(2);
    validar(p, "APROVADO");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from transacao_cliente where origem='REAL' and tipo='PEDIDO'",
                Integer.class))
        .isEqualTo(1);
    var transacoes =
        json.readTree(
            mvc.perform(get("/api/v1/clientes/1/transacoes"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(transacoes.toString()).contains("Pedido real", "[LEGADO]");
    var cliente =
        json.readTree(
            mvc.perform(get("/api/v1/clientes/1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(cliente.path("ranking").asInt()).isEqualTo(5);
  }

  private long cupomCinquenta() {
    return jdbc.queryForObject(
        "insert into cupom(tipo,cliente_id,valor,situacao,criado_em) values ('TROCA',1,50,'ATIVO',current_timestamp) returning id",
        Long.class);
  }

  @Test
  void RN0037_RN0044_concorrenciaPeloMesmoCupomNaoDuplicaPedidoNemReservaDeCredito()
      throws Exception {
    jdbc.update("update produto set preco=33 where id=1");
    adicionar(1);
    long cupom = cupomCinquenta();
    String dados =
        json.writeValueAsString(
            Map.of("enderecoId", 1, "cuponsTroca", List.of(cupom), "cartoes", List.of()));
    var inicio = new java.util.concurrent.CountDownLatch(1);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var tarefas =
          List.of(1, 2).stream()
              .map(
                  i ->
                      pool.submit(
                          () -> {
                            inicio.await();
                            return mvc.perform(
                                    post("/api/v1/clientes/1/pedidos")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(dados))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                          }))
              .toList();
      inicio.countDown();
      assertThat(List.of(tarefas.get(0).get(), tarefas.get(1).get()))
          .containsExactlyInAnyOrder(201, 409);
    }
    assertThat(contar("pedido")).isEqualTo(1);
    assertThat(jdbc.queryForObject("select situacao from cupom where id=?", String.class, cupom))
        .isEqualTo("UTILIZADO");
  }

  @Test
  void RF0037_RN0037_revalidarCupomNaAprovacaoRecusaCreditoQueExpirou() throws Exception {
    jdbc.update("update produto set preco=33 where id=1");
    adicionar(1);
    long cupom = cupomCinquenta();
    long p = vendas.finalizar(1L,
        new CheckoutInput(1L, null, false, null, List.of(cupom), List.of())).id();
    jdbc.update("update cupom set validade='2026-10-11' where id=?", cupom);
    assertThat(validar(p, "APROVADO").path("status").asText()).isEqualTo("REPROVADA");
    assertThat(estoque()).isEqualTo(25);
    assertThat(jdbc.queryForObject("select situacao from cupom where id=?", String.class, cupom))
        .isEqualTo("ATIVO");
  }

  @Test
  void RF0038_idempotenciaDeCheckoutRetornaPedidoEConflitaComComposicaoDiferente()
      throws Exception {
    jdbc.update("update produto set preco=33 where id=1");
    adicionar(1);
    long cupom = cupomCinquenta();
    String dados =
        json.writeValueAsString(
            Map.of("enderecoId", 1, "cuponsTroca", List.of(cupom), "cartoes", List.of()));
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/api/v1/clientes/1/pedidos")
                  .header("Idempotency-Key", "compra-teste")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(dados))
          .andExpect(status().isCreated());
    assertThat(contar("pedido")).isEqualTo(1);
    mvc.perform(
            post("/api/v1/clientes/1/pedidos")
                .header("Idempotency-Key", "compra-teste")
                .contentType(MediaType.APPLICATION_JSON)
                .content(dados.replace("[" + cupom + "]", "[]")))
        .andExpect(status().isConflict());
  }
  @Test void RF0037_RN0037_promocionalVinculadoAOutroClienteERecusadoNoCheckoutENaAprovacao() throws Exception {
    adicionar(1);
    String codigo=jdbc.queryForObject("insert into cupom(tipo,cliente_id,percentual,situacao,criado_em) values ('PROMOCIONAL',2,10,'ATIVO',current_timestamp) returning codigo",String.class);
    postJson("/clientes/1/carrinho/resumo",Map.of("enderecoId",1,"cupomPromocional",codigo),409);
    long p=pedido(1);
    jdbc.update("update pedido set cupom_promocional_id=(select id from cupom where codigo=?) where id=?",codigo,p);
    assertThat(validar(p,"APROVADO").path("motivoResultado").asText()).isEqualTo("CUPOM_INVALIDO");assertThat(estoque()).isEqualTo(25);
  }

  private void carrinhoAntigo() {
    jdbc.execute("insert into carrinho(cliente_id,criado_em,atualizado_em) values(1,current_timestamp,current_timestamp)");
    jdbc.execute("insert into item_carrinho(carrinho_id,produto_id,quantidade) values(1,1,2)");
  }
  @Test void RN0044_migracaoDeCarrinhoPreservaPrazoDaUltimaInclusaoAuditada() throws Exception {
    carrinhoAntigo();var inclusao=clock.instant().atOffset(ZoneOffset.UTC).minusMinutes(10);
    jdbc.update("insert into auditoria_cliente(cliente_id,ocorrida_em,ator,operacao,entidade,entidade_id,alteracoes) values(1,?,'ADMIN_DEMO','ADICIONAR_ITEM_CARRINHO','CARRINHO',1,'Inclusão anterior à V9')",inclusao);
    reservas.processarVencimentos();assertThat(contar("item_carrinho")).isEqualTo(1);
    assertThat(jdbc.queryForObject("select vence_em from reserva_estoque",java.sql.Timestamp.class).toInstant()).isEqualTo(clock.instant().plus(Duration.ofMinutes(10)));
    clock.avancar(Duration.ofMinutes(10));reservas.processarVencimentos();assertThat(contar("item_carrinho")).isZero();assertThat(estoque()).isEqualTo(25);
  }
  @Test void RN0044_migracaoDeCarrinhoSemPrazoAuditavelPreservaMassaParaReinclusao() {
    carrinhoAntigo();reservas.processarVencimentos();reservas.processarVencimentos();
    assertThat(contar("item_carrinho")).isZero();assertThat(contar("reserva_estoque")).isZero();assertThat(estoque()).isEqualTo(25);
    assertThat(jdbc.queryForObject("select motivo from item_retirado_carrinho",String.class)).isEqualTo("REINCLUSAO_NECESSARIA");
    assertThat(contar("notificacao_venda")).isEqualTo(1);
  }

}
