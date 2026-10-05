package br.com.auralab.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.auralab.PostgresTest;
import br.com.auralab.e2e.pages.LojaPage;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Demonstração funcional da Gestão de Vendas na jornada principal da loja (/, /sacola e
 * /checkout), dirigindo o navegador contra a API real e o PostgreSQL de Testcontainers. Cobre o
 * roteiro mínimo do enunciado: carrinho
 * com vários itens e alteração de quantidade (RF0031/RF0032/RN0031), compra com endereço e cartão
 * já cadastrados, compra com novo endereço e cartão incorporados (RF0035/RF0036), pagamento com
 * mais de um cartão respeitando o mínimo (RN0034), cartão abaixo de R$ 10,00 combinado com cupom
 * (RN0035), cupons que superam a compra gerando cupom de troco (RN0036) e pedido finalizado com
 * status EM PROCESSAMENTO (RF0038, RN0033).
 *
 * <p>Marina Costa (cliente 1, endereço 1 em SP) e o produto demonstrativo 1 (R$ 89,90) vêm da massa
 * reiniciada. Frete SP = R$ 15,00 + R$ 2,00/unidade; para 1 unidade o total é R$ 106,90.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VendasLojaIT extends PostgresTest {

  @LocalServerPort int port;
  WebDriver driver;
  LojaPage loja;

  private static final long CLIENTE = 1L;
  private static final long ENDERECO = 1L;
  private static final long PRODUTO = 1L; // Creme Facial Vitamina C — R$ 89,90, estoque 25
  private static final long PRODUTO2 = 2L; // Sérum Ácido Hialurônico — R$ 129,90, estoque 12
  private static final String TOTAL = "106.90"; // 89,90 + frete SP (15 + 2*1)

  @BeforeEach
  void navegador() {
    if (driver == null) {
      var options = new ChromeOptions();
      if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true")))
        options.addArguments("--headless=new");
      options.addArguments("--window-size=1440,1400", "--lang=pt-BR");
      driver = new ChromeDriver(options);
      loja = new LojaPage(driver, "http://localhost:" + port);
    }
    driver.manage().deleteAllCookies();
    driver.get("http://localhost:" + port + "/");
    ((JavascriptExecutor) driver).executeScript("localStorage.clear();sessionStorage.clear();");
    driver.navigate().refresh();
    loja.esperarSelecao();
  }

  @AfterEach
  void evidencia(TestInfo info) throws Exception {
    if (driver != null) {
      Files.createDirectories(Path.of("target/screenshots"));
      String nome = info.getTestMethod().orElseThrow().getName().replaceAll("[^a-zA-Z0-9_-]", "_");
      Files.write(
          Path.of("target/screenshots", nome + ".png"),
          ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }
  }

  @AfterAll
  void encerrar() {
    if (driver != null) driver.quit();
  }

  @Test
  void RF0031_RF0032_RN0031_carrinhoComVariosItensEAlteracaoDeQuantidade() {
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    loja.adicionar(PRODUTO2, 1);
    loja.esperarBadge(2);
    loja.abrirCarrinho();

    long item1 = itemId(PRODUTO);
    loja.atualizarItem(item1, 3);

    assertThat(quantidadeNoCarrinho(PRODUTO)).isEqualTo(3);
    assertThat(jdbc.queryForObject("select count(*) from item_carrinho", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void RN0031_naoAdicionaAcimaDoEstoque() {
    loja.entrar(CLIENTE);
    // Produto 2 tem 12 em estoque; tentar 13 deve ser recusado sem criar item.
    loja.adicionarEsperandoErro(PRODUTO2, 13);
    // Nenhum item é criado — a notificação de erro é exibida (RN0031).
    assertThat(jdbc.queryForObject("select count(*) from item_carrinho", Integer.class)).isZero();
  }

  @Test
  void RF0033_RF0035_RF0036_RF0038_compraComEnderecoECartaoExistentes() {
    long cartao = inserirCartao();
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    loja.abrirCheckout();
    loja.escolherEndereco(ENDERECO);
    loja.valorCartao(cartao, TOTAL);
    loja.finalizar();

    assertThat(loja.temErro()).isFalse();
    assertThat(loja.statusPedido()).contains("EM PROCESSAMENTO");
    assertThat(jdbc.queryForObject("select status from pedido", String.class))
        .isEqualTo("EM_PROCESSAMENTO");
    assertThat(jdbc.queryForObject("select total from pedido", BigDecimal.class))
        .isEqualByComparingTo(TOTAL);
    assertThat(jdbc.queryForObject("select count(*) from item_carrinho", Integer.class)).isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auditoria_cliente where operacao='FINALIZAR_COMPRA'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void RF0035_RF0036_compraComNovoEnderecoENovoCartaoIncorporados() {
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    loja.abrirCheckout();
    loja.escolherNovoEndereco();
    loja.preencherNovoEndereco("Trabalho", "SP");
    loja.salvarEnderecoNoPerfil();
    loja.abrirNovoCartao();
    loja.preencherNovoCartao("4111111111111111", "MARINA COSTA", "Visa", "123", TOTAL);
    loja.salvarCartaoNoPerfil();
    loja.finalizar();

    assertThat(loja.temErro()).isFalse();
    assertThat(jdbc.queryForObject("select status from pedido", String.class))
        .isEqualTo("EM_PROCESSAMENTO");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from endereco where cliente_id=1 and apelido='Trabalho'",
                Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from cartao where cliente_id=1", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select ultimos_quatro from cartao where cliente_id=1", String.class))
        .isEqualTo("1111");
  }

  @Test
  void RN0034_pagamentoComDoisCartoesRespeitandoOMinimo() {
    long c1 = inserirCartao();
    long c2 = inserirCartao();
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    loja.abrirCheckout();
    loja.escolherEndereco(ENDERECO);

    // Um cartão abaixo de R$ 10,00 sem cupons deve ser recusado (RN0034).
    loja.valorCartao(c1, "101.90");
    loja.valorCartao(c2, "5.00");
    loja.finalizar();
    assertThat(loja.temErro()).isTrue();
    assertThat(jdbc.queryForObject("select count(*) from pedido", Integer.class)).isZero();

    // Corrige para dois cartões acima do mínimo, somando o total.
    loja.valorCartao(c1, "96.90");
    loja.valorCartao(c2, "10.00");
    loja.finalizar();
    assertThat(loja.temErro()).isFalse();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from pagamento_parte where tipo='CARTAO'", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void RN0035_cartaoAbaixoDe10ComCupomDeTroca() {
    long cartao = inserirCartao();
    long cupom = inserirCupomTroca("101.90"); // total 106,90 - 5,00 no cartão
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    loja.abrirCheckout();
    loja.escolherEndereco(ENDERECO);
    loja.marcarCupom(cupom);
    loja.valorCartao(cartao, "5.00");
    loja.finalizar();

    assertThat(loja.temErro()).isFalse();
    assertThat(jdbc.queryForObject("select status from pedido", String.class))
        .isEqualTo("EM_PROCESSAMENTO");
    assertThat(jdbc.queryForObject("select situacao from cupom where id=?", String.class, cupom))
        .isEqualTo("UTILIZADO");
  }

  @Test
  void RN0036_cuponsSuperamOTotalEGeramCupomDeTroco() {
    long c1 = inserirCupomTroca("70.00");
    long c2 = inserirCupomTroca("70.00"); // 140,00 > 106,90; troco 33,10
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    loja.abrirCheckout();
    loja.escolherEndereco(ENDERECO);
    loja.marcarCupom(c1);
    loja.marcarCupom(c2);
    loja.finalizar();

    assertThat(loja.temErro()).isFalse();
    assertThat(loja.texto()).contains("EM PROCESSAMENTO");
    assertThat(
            jdbc.queryForObject(
                "select valor from cupom where cliente_id=1 and origem like 'Troco%'",
                BigDecimal.class))
        .isEqualByComparingTo("33.10");
  }

  @Test
  void RF0038_RN0033_pedidoFinalizadoEmProcessamentoComCupomPromocional() {
    long cartao = inserirCartao();
    String promo = inserirCupomPromocional("10.00");
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    loja.abrirCheckout();
    loja.escolherEndereco(ENDERECO);
    loja.promocional(promo);
    loja.calcularResumo();
    loja.valorCartao(cartao, "97.91"); // 89,90 - 8,99 (10%) + 17,00 de frete
    loja.finalizar();

    assertThat(loja.temErro()).isFalse();
    assertThat(loja.statusPedido()).contains("EM PROCESSAMENTO");
    assertThat(jdbc.queryForObject("select status from pedido", String.class))
        .isEqualTo("EM_PROCESSAMENTO");
    assertThat(jdbc.queryForObject("select desconto from pedido", BigDecimal.class))
        .isEqualByComparingTo("8.99");
  }

  // ---- massa auxiliar via JDBC (funcionalidades de cadastro já implementadas / pré-carga) ----

  private long inserirCartao() {
    return jdbc.queryForObject(
        "insert into cartao(cliente_id,bandeira_id,titular,ultimos_quatro,preferencial,ativo)"
            + " values (1,1,'MARINA COSTA','1111',false,true) returning id",
        Long.class);
  }

  private long inserirCupomTroca(String valor) {
    return jdbc.queryForObject(
        "insert into cupom(tipo,cliente_id,valor,situacao,criado_em) values"
            + " ('TROCA',1,?,'ATIVO', TIMESTAMPTZ '2026-09-01 12:00:00+00') returning id",
        Long.class,
        new BigDecimal(valor));
  }

  private String inserirCupomPromocional(String percentual) {
    return jdbc.queryForObject(
        "insert into cupom(tipo,percentual,situacao,criado_em) values"
            + " ('PROMOCIONAL',?,'ATIVO', TIMESTAMPTZ '2026-09-01 12:00:00+00') returning codigo",
        String.class,
        new BigDecimal(percentual));
  }

  private Long itemId(long produtoId) {
    return jdbc.queryForObject(
        "select ic.id from item_carrinho ic join carrinho c on c.id=ic.carrinho_id"
            + " where c.cliente_id=1 and ic.produto_id=?",
        Long.class,
        produtoId);
  }

  private int quantidadeNoCarrinho(long produtoId) {
    return jdbc.queryForObject(
        "select ic.quantidade from item_carrinho ic join carrinho c on c.id=ic.carrinho_id"
            + " where c.cliente_id=1 and ic.produto_id=?",
        Integer.class,
        produtoId);
  }
}
