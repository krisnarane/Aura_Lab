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
 * Demonstração funcional da Gestão de Vendas na jornada principal da loja (/, /sacola e /checkout),
 * dirigindo o navegador contra a API real e o PostgreSQL de Testcontainers. Cobre o roteiro mínimo
 * do enunciado: carrinho com vários itens e alteração de quantidade (RF0031/RF0032/RN0031), compra
 * com endereço e cartão já cadastrados, compra com novo endereço e cartão incorporados
 * (RF0035/RF0036), pagamento com mais de um cartão respeitando o mínimo (RN0034), cartão abaixo de
 * R$ 10,00 combinado com cupom (RN0035), cupons que superam a compra gerando cupom de troco
 * (RN0036) e pedido finalizado com status EM PROCESSAMENTO (RF0038, RN0033).
 *
 * <p>Marina Costa (cliente 1, endereço 1 em SP) e o produto demonstrativo 1 (R$ 89,90) vêm da massa
 * reiniciada. Frete SP = R$ 15,00 + R$ 2,00/unidade; para 1 unidade o total é R$ 106,90.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@org.springframework.context.annotation.Import(br.com.auralab.TempoVendaTeste.class)
class VendasLojaIT extends PostgresTest {

  @LocalServerPort int port;
  WebDriver driver;
  LojaPage loja;

  @org.springframework.beans.factory.annotation.Autowired
  br.com.auralab.TempoVendaTeste.Relogio relogio;

  @org.springframework.beans.factory.annotation.Autowired
  br.com.auralab.vendas.service.ReservaEstoqueService reservas;

  private static final long CLIENTE = 1L;
  private static final long ENDERECO = 1L;
  private static final long PRODUTO = 1L; // Creme Facial Vitamina C — R$ 89,90, estoque 25
  private static final long PRODUTO2 = 2L; // Sérum Ácido Hialurônico — R$ 129,90, estoque 12
  private static final String TOTAL = "106.90"; // 89,90 + frete SP (15 + 2*1)

  @BeforeEach
  void navegador() {
    relogio.reiniciar();
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
    // Aguarda os fetches da vitrine: renderizarVendasApi regrava a chave do cliente
    // selecionado de forma assíncrona, e limpar antes disso causava seleção fantasma.
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
        .until(
            org.openqa.selenium.support.ui.ExpectedConditions.or(
                org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(
                    org.openqa.selenium.By.cssSelector("[data-testid='ir-carrinho']")),
                org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(
                    org.openqa.selenium.By.cssSelector("[data-entrar-cliente]"))));
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
        .isEqualTo("RESERVADO");
    validarPeloNavegador("APROVADO");
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
    validarPeloNavegador("APROVADO");
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

  // ---- casos do plano: operações pelo navegador, pós-condições no PostgreSQL ----

  @Test
  @DisplayName("CT032 - Recusar produto com estoque zero sem alterar o carrinho")
  void RF0031_RN0031_CT032_produtoSemEstoqueNaoEntraNoCarrinho() {
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO2, 1);
    loja.esperarBadge(1);
    var carrinhoAntes = jdbc.queryForList("select * from item_carrinho order by id");
    var cabecalhoAntes = jdbc.queryForList("select * from carrinho order by id");
    int auditoriasAntes = auditoriasCliente();
    // Simula esgotamento depois de carregar a vitrine. A tentativa passa pelo botão real,
    // com a API consultando estoque zero no PostgreSQL; nenhum endpoint é chamado pelo teste.
    jdbc.update("update produto set estoque=0 where id=?", PRODUTO);
    assertThat(
            jdbc.queryForObject("select estoque from produto where id=?", Integer.class, PRODUTO))
        .isZero();

    loja.adicionarEsperandoErro(PRODUTO, 1);
    assertThat(loja.toastErro()).contains("0 unidade(s) em estoque");
    assertThat(jdbc.queryForList("select * from item_carrinho order by id"))
        .isEqualTo(carrinhoAntes);
    assertThat(jdbc.queryForList("select * from carrinho order by id")).isEqualTo(cabecalhoAntes);
    assertThat(auditoriasCliente()).isEqualTo(auditoriasAntes);
    loja.abrirProduto(PRODUTO);
    assertThat(loja.texto()).contains("Esgotado");
    assertThat(loja.adicaoHabilitada(PRODUTO)).isFalse();
  }

  @Test
  @DisplayName("CT035 - Recusar quantidade zero e manter o item na sacola")
  void RF0032_CT035_quantidadeZeroNaoAlteraCarrinho() {
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 2);
    loja.esperarBadge(2);
    loja.abrirCarrinho();
    long item = itemId(PRODUTO);
    var itensAntes = jdbc.queryForList("select * from item_carrinho order by id");
    var carrinhoAntes = jdbc.queryForList("select * from carrinho order by id");
    int auditoriasAntes = auditoriasCliente();
    assertThat(loja.quantidadeItem(item)).isEqualTo("2");

    loja.atualizarItemEsperandoErro(item, 0);
    assertThat(loja.toastErro()).isNotBlank();
    assertThat(jdbc.queryForList("select * from item_carrinho order by id")).isEqualTo(itensAntes);
    assertThat(jdbc.queryForList("select * from carrinho order by id")).isEqualTo(carrinhoAntes);
    assertThat(auditoriasCliente()).isEqualTo(auditoriasAntes);
    loja.abrirCarrinho();
    assertThat(loja.quantidadeItem(item)).isEqualTo("2");
    assertThat(loja.texto()).contains("Remover");
  }

  @Test
  @DisplayName("CT038 - Impedir checkout com sacola vazia")
  void RF0033_CT038_carrinhoVazioNaoIniciaCompra() {
    loja.entrar(CLIENTE);
    assertThat(jdbc.queryForObject("select count(*) from item_carrinho", Integer.class)).isZero();
    int auditoriasAntes = auditoriasCliente();
    loja.abrirCarrinho();
    assertThat(loja.texto()).contains("Sua sacola está vazia");
    assertThat(loja.checkoutDisponivel()).isFalse();

    // A sacola vazia não oferece botão de checkout; a rota existente também deve protegê-lo.
    loja.abrirCheckoutVazio();
    assertThat(loja.texto()).contains("Sua sacola está vazia");
    assertThat(loja.formularioCheckoutPresente()).isFalse();
    conferirSemPedidoNovo(auditoriasAntes);
    assertThat(jdbc.queryForObject("select count(*) from item_carrinho", Integer.class)).isZero();
  }

  @Test
  @DisplayName("CT039 - Impedir compra quando o estoque do item chega a zero")
  void RF0033_RN0031_RN0032_CT039_itemEsgotadoNaoFinalizaCompra() {
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    jdbc.update("update produto set estoque=0 where id=?", PRODUTO);
    loja.abrirCarrinho();
    assertThat(loja.texto()).contains("Sua sacola está vazia", "SEM_ESTOQUE");
    assertThat(loja.checkoutDisponivel()).isFalse();
    assertThat(jdbc.queryForObject("select count(*) from item_carrinho", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from pedido", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from notificacao_venda", Integer.class))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("CT041 - Exigir endereço válido para recalcular frete e finalizar")
  void RF0034_CT041_semEnderecoNaoCalculaFrete() {
    // Sem endereço de entrega disponível, a tela existente pede um novo endereço.
    long cartao = prepararCheckoutSemEndereco();
    var itensAntes = jdbc.queryForList("select * from item_carrinho order by id");
    var enderecosAntes = jdbc.queryForList("select * from endereco order by id");
    int auditoriasAntes = auditoriasCliente();
    BigDecimal totalAntes = loja.totalResumo();
    BigDecimal freteAntes = loja.freteResumo();

    loja.calcularResumo();
    assertThat(loja.toastErro()).containsIgnoringCase("endereço");
    assertThat(loja.totalResumo()).isEqualByComparingTo(totalAntes);
    assertThat(loja.freteResumo()).isEqualByComparingTo(freteAntes);
    loja.valorCartao(cartao, TOTAL);
    loja.finalizar();
    assertThat(loja.erro()).isNotBlank();
    conferirSemPedidoNovo(auditoriasAntes);
    assertThat(jdbc.queryForList("select * from item_carrinho order by id")).isEqualTo(itensAntes);
    assertThat(jdbc.queryForList("select * from endereco order by id")).isEqualTo(enderecosAntes);
  }

  @Test
  @DisplayName("CT042 - Recalcular frete e total entre endereços SP e BA")
  void RF0034_CT042_recalculaFreteAoMudarEndereco() {
    long bahia =
        jdbc.queryForObject(
            "insert into endereco(cliente_id,apelido,tipo_residencia,tipo_logradouro,logradouro,"
                + "numero,bairro,cep,cidade,estado,pais,cobranca,entrega,preferencial_entrega,ativo)"
                + " values (1,'Casa BA','Casa','Rua','Teste','10','Centro','40020000','Salvador',"
                + "'BA','Brasil',false,true,false,true) returning id",
            Long.class);
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    var itensAntes = jdbc.queryForList("select * from item_carrinho order by id");
    var enderecosAntes = jdbc.queryForList("select * from endereco order by id");
    int auditoriasAntes = auditoriasCliente();
    loja.abrirCheckout();

    loja.escolherEndereco(ENDERECO).calcularResumo();
    // Valores da política atual do projeto; o DRS RF0034 não define a fórmula.
    assertThat(loja.freteResumo()).isEqualByComparingTo("17.00");
    assertThat(loja.totalResumo()).isEqualByComparingTo("106.90");
    loja.escolherEndereco(bahia).calcularResumo();
    assertThat(loja.freteResumo()).isEqualByComparingTo("32.00");
    assertThat(loja.totalResumo()).isEqualByComparingTo("121.90");
    loja.escolherEndereco(ENDERECO).calcularResumo();
    assertThat(loja.freteResumo()).isEqualByComparingTo("17.00");
    assertThat(loja.totalResumo()).isEqualByComparingTo("106.90");

    conferirSemPedidoNovo(auditoriasAntes);
    assertThat(jdbc.queryForList("select * from item_carrinho order by id")).isEqualTo(itensAntes);
    assertThat(jdbc.queryForList("select * from endereco order by id")).isEqualTo(enderecosAntes);
  }

  @Test
  @DisplayName("CT053 - Recusar finalização sem endereço e preservar a sacola")
  void RF0038_CT053_semEnderecoNaoFinalizaCompra() {
    long cartao = prepararCheckoutSemEndereco();
    var itensAntes = jdbc.queryForList("select * from item_carrinho order by id");
    var enderecosAntes = jdbc.queryForList("select * from endereco order by id");
    int auditoriasAntes = auditoriasCliente();
    loja.valorCartao(cartao, TOTAL);

    loja.finalizar();
    assertThat(loja.erro()).isNotBlank();
    assertThat(loja.formularioCheckoutPresente()).isTrue();
    conferirSemPedidoNovo(auditoriasAntes);
    assertThat(jdbc.queryForList("select * from item_carrinho order by id")).isEqualTo(itensAntes);
    assertThat(jdbc.queryForList("select * from endereco order by id")).isEqualTo(enderecosAntes);
    loja.abrirCarrinho();
    assertThat(loja.quantidadeItem(itemId(PRODUTO))).isEqualTo("1");
    assertThat(loja.checkoutDisponivel()).isTrue();
  }

  // ---- massa auxiliar via JDBC (somente banco descartável de Testcontainers) ----

  private long prepararCheckoutSemEndereco() {
    jdbc.update("update endereco set entrega=false, preferencial_entrega=false where cliente_id=1");
    assertThat(jdbc.queryForObject("select ativo from cliente where id=1", Boolean.class)).isTrue();
    long cartao = inserirCartao();
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    loja.abrirCheckout();
    assertThat(driver.findElement(By.cssSelector("[data-testid='endereco-novo']")).isSelected())
        .isTrue();
    assertThat(driver.findElement(By.name("endereco_estado")).getDomProperty("value")).isEmpty();
    assertThat(driver.findElements(By.cssSelector("[data-testid='endereco-1']"))).isEmpty();
    return cartao;
  }

  private int auditoriasCliente() {
    return jdbc.queryForObject("select count(*) from auditoria_cliente", Integer.class);
  }

  private void conferirSemPedidoNovo(int auditoriasAntes) {
    assertThat(jdbc.queryForObject("select count(*) from pedido", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from item_pedido", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from pagamento_parte", Integer.class)).isZero();
    assertThat(auditoriasCliente()).isEqualTo(auditoriasAntes);
  }

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

  private WebElement aguardar(String seletor) {
    return new org.openqa.selenium.support.ui.WebDriverWait(
            driver, java.time.Duration.ofSeconds(20)).ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(
            org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(seletor)));
  }

  /** Centraliza o alvo e espera que esteja desobstruído antes do clique nativo. */
  private void clicar(String seletor) {
    br.com.auralab.e2e.pages.InteracaoNavegador.clicar(driver, aguardar(seletor));
  }

  private long compraPeloNavegador(int quantidade) {
    long cartao = inserirCartao();
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, quantidade);
    loja.esperarBadge(quantidade);
    loja.abrirCheckout();
    loja.escolherEndereco(ENDERECO);
    loja.valorCartao(
        cartao,
        new BigDecimal("89.90")
            .multiply(BigDecimal.valueOf(quantidade))
            .add(new BigDecimal("15.00"))
            .add(BigDecimal.valueOf(quantidade * 2))
            .toPlainString());
    loja.finalizar();
    return jdbc.queryForObject("select max(id) from pedido", Long.class);
  }

  private void validarPeloNavegador(String resultado) {
    long id = jdbc.queryForObject("select max(id) from pedido", Long.class);
    loja.abrir("/admin/pedidos/" + id);
    new org.openqa.selenium.support.ui.Select(aguardar("[name='resultadoSimulado']"))
        .selectByValue(resultado);
    clicar("[data-testid='validar-pagamento'] button");
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(20)).ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(
            d ->
                aguardar("[data-testid='detalhe-pedido']")
                    .getText()
                    .contains("APROVADO".equals(resultado) ? "APROVADA" : "REPROVADA"));
  }

  private void entregarPeloNavegador(long p) {
    validarPeloNavegador("APROVADO");
    clicar("[data-despachar]");
    clicar("[data-entregar]");
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(20)).ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(d -> aguardar("[data-testid='detalhe-pedido']").getText().contains("ENTREGUE"));
  }

  @Test
  void RF0039_RF0040_RF0053_CT055_compraAprovadaDespachadaEEntregueNasTelasExistentes() {
    long p = compraPeloNavegador(1);
    assertThat(aguardar(".confirmation-heading").getCssValue("text-align")).isEqualTo("center");
    var painel = aguardar("[data-testid='pedido-confirmado']").getRect();
    var titulo = aguardar(".confirmation-heading").getRect();
    assertThat(Math.abs((painel.x + painel.width / 2.0) - (titulo.x + titulo.width / 2.0)))
        .isLessThan(2.0);
    assertThat(jdbc.queryForObject("select estoque from produto where id=1", Integer.class))
        .isEqualTo(25);
    entregarPeloNavegador(p);
    assertThat(jdbc.queryForObject("select estoque from produto where id=1", Integer.class))
        .isEqualTo(24);
    assertThat(jdbc.queryForObject("select count(*) from movimentacao_estoque", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void RN0038_CT056_pagamentoRecusadoLiberaSemBaixar() {
    compraPeloNavegador(1);
    validarPeloNavegador("REPROVADO");
    assertThat(jdbc.queryForObject("select estoque from produto where id=1", Integer.class))
        .isEqualTo(25);
    assertThat(driver.findElements(By.cssSelector("[data-despachar]")).size()).isZero();
  }

  @Test
  void RN0044_RN0045_RNF0042_CT057_avisoExpiracaoENovaInclusao() {
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    loja.abrirCarrinho();
    relogio.avancar(java.time.Duration.ofMinutes(15));
    reservas.processarVencimentos();
    loja.abrir("/sacola");
    assertThat(aguardar("[data-testid='aviso-reserva']").getText()).contains("cinco minutos");
    relogio.avancar(java.time.Duration.ofMinutes(5));
    reservas.processarVencimentos();
    loja.abrir("/sacola");
    assertThat(aguardar("[data-testid='item-retirado']").getText())
        .contains("PRAZO_EXPIRADO", "20 minutos");
    assertThat(loja.checkoutDisponivel()).isFalse();
    loja.abrir("/");
    aguardar("[data-add]");
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    assertThat(
            jdbc.queryForObject("select readicionado from item_retirado_carrinho", Boolean.class))
        .isTrue();
    loja.abrirCarrinho();
    aguardar("[data-testid='ir-checkout']");
    assertThat(driver.findElements(By.cssSelector("[data-testid='aviso-reserva']"))).isEmpty();
    assertThat(driver.findElements(By.cssSelector("[data-testid='item-retirado']"))).isEmpty();
  }

  @Test
  void RF0041_RF0042_RF0043_RF0044_RF0045_RF0054_RN0046_CT058_CT059_trocasParcialEIntegralPelasTelas() {
    long p = compraPeloNavegador(2);
    entregarPeloNavegador(p);
    loja.abrir("/perfil/pedidos");
    clicar("[data-testid='abrir-pedido-" + p + "']");
    aguardar("[data-testid='solicitar-troca']");
    new org.openqa.selenium.support.ui.Select(aguardar("[name='itemPedidoId']")).selectByIndex(1);
    aguardar("[name='motivo']").sendKeys("Avaria fictícia");
    clicar("[data-testid='solicitar-troca'] button");
    aguardar("[data-testid='troca-1']");
    loja.abrir("/admin/trocas");
    clicar("[data-autorizar]");
    new org.openqa.selenium.support.ui.Select(aguardar("[name='retornarEstoque']"))
        .selectByValue("true");
    clicar("[data-testid='receber-troca'] button");
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(20)).ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(d -> aguardar("[data-testid='troca-1']").getText().contains("TROCADO"));
    assertThat(jdbc.queryForObject("select estoque from produto where id=1", Integer.class))
        .isEqualTo(24);
    assertThat(jdbc.queryForObject("select status from pedido where id=?", String.class, p))
        .isEqualTo("ENTREGUE");
    loja.abrir("/perfil/pedidos/" + p);
    assertThat(aguardar("[data-testid='detalhe-pedido']").getText()).contains("ENTREGUE");
    assertThat(driver.getPageSource()).contains("autorizada");
    long credito =
        jdbc.queryForObject("select id from cupom where origem like 'Troca recebida%'", Long.class);
    long cartao = inserirCartao();
    loja.abrir("/");
    aguardar("[data-add]");
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    loja.abrirCheckout();
    loja.escolherEndereco(ENDERECO);
    loja.marcarCupom(credito);
    loja.valorCartao(cartao, "17.00");
    loja.finalizar();
    long segundo = jdbc.queryForObject("select max(id) from pedido", Long.class);
    entregarPeloNavegador(segundo);
    assertThat(jdbc.queryForObject("select situacao from cupom where id=?", String.class, credito))
        .isEqualTo("UTILIZADO");
    loja.abrir("/perfil/pedidos/" + segundo);
    aguardar("[name='motivo']").sendKeys("Troca integral fictícia");
    clicar("[data-testid='solicitar-troca'] button");
    aguardar("[data-testid='troca-2']");
    loja.abrir("/admin/trocas");
    clicar("[data-autorizar]");
    new org.openqa.selenium.support.ui.Select(aguardar("[name='retornarEstoque']"))
        .selectByValue("false");
    clicar("[data-testid='receber-troca'] button");
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(20)).ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(d -> aguardar("[data-testid='troca-2']").getText().contains("TROCADO"));
    assertThat(jdbc.queryForObject("select status from pedido where id=?", String.class, segundo))
        .isEqualTo("TROCADO");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from cupom where origem like 'Troca recebida%'", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void RN0032_CT060_mudancaDeEstoqueAjustaESacolaSincronizaAutomaticamente() {
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 3);
    loja.esperarBadge(3);
    loja.abrirCarrinho();
    long item = itemId(PRODUTO);
    jdbc.update("update produto set estoque=2 where id=1");
    new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(20)).ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(
            d ->
                "2"
                    .equals(
                        aguardar("[data-testid='qtd-item-" + item + "']").getDomProperty("value")));
    jdbc.update("update produto set estoque=0 where id=1");
    aguardar("[data-testid='carrinho-vazio']");
    assertThat(loja.checkoutDisponivel()).isFalse();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "20.00,35.00,5.00",
    "20.00,40.00,10.00",
    "40.00,35.00,25.00"
  })
  void RN0036_CT061_exemplosMonetariosDoDRSNaTela(String primeiro, String segundo, String troco) {
    jdbc.update("update produto set preco=33.00 where id=1");
    long a = inserirCupomTroca(primeiro), b = inserirCupomTroca(segundo);
    loja.entrar(CLIENTE);
    loja.adicionar(PRODUTO, 1);
    loja.esperarBadge(1);
    loja.abrirCheckout();
    loja.escolherEndereco(ENDERECO);
    loja.marcarCupom(a);
    loja.marcarCupom(b);
    loja.finalizar();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from cupom where origem like 'Troco%'", Integer.class))
        .isZero();
    validarPeloNavegador("APROVADO");
    assertThat(
            jdbc.queryForObject(
                "select valor from cupom where origem like 'Troco%'", BigDecimal.class))
        .isEqualByComparingTo(troco);
  }
}
