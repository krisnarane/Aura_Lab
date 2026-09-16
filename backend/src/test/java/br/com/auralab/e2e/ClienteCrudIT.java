package br.com.auralab.e2e;

import static org.assertj.core.api.Assertions.*;

import br.com.auralab.PostgresTest;
import br.com.auralab.e2e.pages.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClienteCrudIT extends PostgresTest {
  @LocalServerPort int port;
  WebDriver driver;
  ClientesAdminPage page;

  @BeforeEach
  void navegador() {
    if (driver != null) {
      driver.get("http://localhost:" + port + "/admin/clientes");
      ((JavascriptExecutor) driver).executeScript("localStorage.clear();sessionStorage.clear();");
      driver.manage().deleteAllCookies();
      driver.get("about:blank");
      driver.manage().window().setSize(new Dimension(1440, 1200));
      return;
    }
    var options = new ChromeOptions();
    if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true")))
      options.addArguments("--headless=new");
    options.addArguments("--window-size=1440,1200", "--lang=pt-BR");
    driver = new ChromeDriver(options);
    page = new ClientesAdminPage(driver, "http://localhost:" + port);
  }

  @AfterEach
  void fechar(TestInfo info) throws Exception {
    if (driver != null)
      try {
        Files.createDirectories(Path.of("target/screenshots"));
        String nome =
            (info.getTestMethod().orElseThrow().getName() + "-" + info.getDisplayName())
                .replaceAll("[^a-zA-Z0-9_-]", "_");
        Files.write(
            Path.of("target/screenshots", nome + ".png"),
            ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
      } finally {
        // The next scenario reloads the page and resets storage and database.
      }
  }

  @Test
  void RF0021_RN0021_RN0022_RN0026_RNF0035_cadastroPersistenteECodigos() {
    page.cadastro().enviar();
    page.esperar("tabela-clientes");
    assertThat(page.texto()).contains("Cliente Teste", "CLI-000003");
    page.abrir("", "tabela-clientes");
    assertThat(page.texto()).contains("Cliente Teste");
    assertThat(jdbc.queryForObject("select count(distinct codigo) from cliente", Integer.class))
        .isEqualTo(3);
    page.detalhe(3);
    assertThat(page.texto()).contains("Cobrança", "Entrega", "Preferencial");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "nome",
        "cpf",
        "email",
        "genero",
        "nascimento",
        "telefoneDdd",
        "telefoneNumero",
        "senha",
        "endereco_apelido",
        "endereco_tipoResidencia",
        "endereco_tipoLogradouro",
        "endereco_logradouro",
        "endereco_numero",
        "endereco_bairro",
        "endereco_cep",
        "endereco_cidade",
        "endereco_estado",
        "endereco_pais"
      })
  void RF0021_RN0023_RN0026_obrigatorios(String campo) {
    var f = page.cadastro();
    f.campo(campo, "").enviar();
    f.erro();
    assertThat(jdbc.queryForObject("select count(*) from cliente", Integer.class)).isEqualTo(2);
    assertThat(jdbc.queryForObject("select count(*) from auditoria_cliente", Integer.class))
        .isZero();
  }

  @ParameterizedTest
  @CsvSource({
    "cpf,11111111111",
    "email,invalido",
    "telefoneDdd,1",
    "telefoneNumero,123",
    "endereco_cep,123",
    "cpf,52998224725",
    "email,inativo@auralab.dev"
  })
  void RF0021_validacoesEDuplicidadeInativos(String campo, String valor) {
    var f = page.cadastro();
    f.campo(campo, valor).enviar();
    f.erro();
    assertThat(f.valor("nome")).isEqualTo("Cliente Teste");
    assertThat(f.valor("senha")).isEmpty();
    assertThat(jdbc.queryForObject("select count(*) from cliente", Integer.class)).isEqualTo(2);
  }

  @ParameterizedTest
  @CsvSource({
    "nome,Marina",
    "cpf,12345678909",
    "email,marina@auralab",
    "codigo,CLI-000001",
    "genero,Feminino",
    "telefoneTipo,Celular",
    "telefoneDdd,11",
    "telefoneNumero,987654321"
  })
  void RF0024_filtrosIsolados(String campo, String valor) {
    page.abrir("", "tabela-clientes");
    page.filtrar(campo, valor);
    assertThat(page.texto()).contains("Marina Costa");
    if (!campo.equals("telefoneTipo")) assertThat(page.texto()).doesNotContain("Cliente Inativo");
  }

  @Test
  void RF0024_filtrosCombinadosVazioESituacao() {
    page.abrir("", "tabela-clientes");
    assertThat(page.texto()).contains("Cliente Inativo", "Marina Costa");
    var f = new FormularioComponente(driver, "filtros-clientes");
    f.campo("email", "marina");
    page.filtrar("nome", "Marina");
    assertThat(page.texto()).contains("Marina Costa").doesNotContain("Cliente Inativo");
    page.filtrar("codigo", "CLI-00000");
    page.esperar("sem-resultados");
    page.abrir("", "tabela-clientes");
    page.filtrar("nome", "%");
    page.esperar("sem-resultados");
    page.abrir("?ativo=false", "tabela-clientes");
    assertThat(page.texto()).contains("Cliente Inativo").doesNotContain("Marina Costa");
  }

  @Test
  void RF0022_RNF0012_alteracaoPreservaVinculosEAudita() {
    String hash = jdbc.queryForObject("select senha_hash from cliente where id=1", String.class);
    page.editar(1).campo("nome", "Marina Editada").enviar();
    page.esperar("tabela-clientes");
    page.detalhe(1);
    assertThat(page.texto()).contains("Marina Editada", "CLI-000001", "PED-2026-001", "Casa");
    assertThat(jdbc.queryForObject("select senha_hash from cliente where id=1", String.class))
        .isEqualTo(hash);
    String antes =
        jdbc.queryForObject(
            "select dados_anteriores::text from auditoria_cliente where operacao='ALTERAR_CLIENTE'",
            String.class);
    assertThat(antes).contains("Marina Costa");
  }

  @Test
  void RF0022_duplicidadeNaoAltera() {
    var f = page.editar(1);
    f.campo("email", "inativo@auralab.dev").enviar();
    f.erro();
    assertThat(jdbc.queryForObject("select email from cliente where id=1", String.class))
        .isEqualTo("marina@auralab.dev");
    assertThat(jdbc.queryForObject("select count(*) from auditoria_cliente", Integer.class))
        .isZero();
  }

  @Test
  void RF0023_RF0025_cancelarConfirmarRepetir() {
    page.detalhe(1);
    page.clicar("[data-inativar-cliente]");
    new ConfirmacaoComponente(driver).responder(false);
    assertThat(jdbc.queryForObject("select ativo from cliente where id=1", Boolean.class)).isTrue();
    var velho = page.esperar("form-endereco");
    page.clicar("[data-inativar-cliente]");
    new ConfirmacaoComponente(driver).responder(true);
    page.aguardarAtualizacao(velho);
    assertThat(page.texto()).contains("INATIVO", "PED-2026-001", "Casa");
    page.clicar("[data-inativar-cliente]");
    new ConfirmacaoComponente(driver).responder(true);
    page.aguardarTexto("já está inativo");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auditoria_cliente where operacao='INATIVAR_CLIENTE'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void RF0026_RNF0034_variosEnderecosEdicaoEPreferencia() {
    page.detalhe(1);
    new EnderecoComponente(driver)
        .preencher("Trabalho")
        .marcar("cobranca", true)
        .marcar("preferencialEntrega", true)
        .enviar();
    page.esperar("endereco-3");
    page.abrir("/1?editEndereco=3", "form-endereco");
    new EnderecoComponente(driver).campo("complemento", "Sala 5").enviar();
    assertThat(jdbc.queryForObject("select complemento from endereco where id=3", String.class))
        .isEqualTo("Sala 5");
    assertThat(jdbc.queryForObject("select logradouro from endereco where id=1", String.class))
        .isEqualTo("das Flores");
    var velho = page.esperar("form-endereco");
    page.clicar("[data-inativar-endereco='3']");
    page.aguardarAtualizacao(velho);
    assertThat(
            jdbc.queryForObject(
                "select preferencial_entrega from endereco where id=1", Boolean.class))
        .isTrue();
  }

  @Test
  void RN0021_RN0022_protegeMinimos() {
    page.detalhe(1);
    page.clicar("[data-inativar-endereco='1']");
    page.aguardarTexto("ao menos um endereço de cobrança");
    new EnderecoComponente(driver)
        .preencher("Cobrança")
        .marcar("cobranca", true)
        .marcar("entrega", false)
        .enviar();
    page.esperar("endereco-3");
    page.clicar("[data-inativar-endereco='1']");
    page.aguardarTexto("ao menos um endereço de entrega");
    assertThat(jdbc.queryForObject("select ativo from endereco where id=1", Boolean.class))
        .isTrue();
  }

  @Test
  void RF0027_RN0024_RN0025_cartoesPreferenciaEMascara() {
    page.detalhe(1);
    new CartaoComponente(driver).preencher().enviar();
    page.esperar("cartao-1");
    assertThat(page.texto()).contains("Visa final 1111").doesNotContain("4111111111111111");
    new CartaoComponente(driver).preencher().enviar();
    page.esperar("cartao-2");
    var velho = page.esperar("form-cartao");
    page.clicar("[data-preferir-cartao='2']");
    page.aguardarAtualizacao(velho);
    assertThat(jdbc.queryForObject("select id from cartao where preferencial", Long.class))
        .isEqualTo(2);
    velho = page.esperar("form-cartao");
    page.clicar("[data-inativar-cartao='2']");
    page.aguardarAtualizacao(velho);
    assertThat(jdbc.queryForObject("select id from cartao where preferencial", Long.class))
        .isEqualTo(1);
  }

  @ParameterizedTest
  @CsvSource({"numero,123", "titular,''", "codigoSeguranca,1"})
  void RF0027_RN0024_recusaCartao(String campo, String valor) {
    page.detalhe(1);
    var f = new CartaoComponente(driver).preencher();
    f.campo(campo, valor).enviar();
    f.erro();
    assertThat(jdbc.queryForObject("select count(*) from cartao", Integer.class)).isZero();
    assertThat(f.valor("numero")).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
    "Ab@1,Ab@1",
    "abcdefgh@,abcdefgh@",
    "ABCDEFGH@,ABCDEFGH@",
    "Abcdefghi,Abcdefghi",
    "Senha@123,Diferente@123"
  })
  void RF0028_RNF0031_RNF0032_forcaEConfirmacao(String senha, String confirmacao) {
    String antes = jdbc.queryForObject("select senha_hash from cliente where id=1", String.class);
    page.detalhe(1);
    var f = new FormularioComponente(driver, "form-senha");
    f.campo("novaSenha", senha).campo("confirmacaoSenha", confirmacao).enviar();
    f.erro();
    assertThat(jdbc.queryForObject("select senha_hash from cliente where id=1", String.class))
        .isEqualTo(antes);
  }

  @Test
  void RF0028_alteracaoIsolada() {
    page.detalhe(1);
    new FormularioComponente(driver, "form-senha")
        .campo("novaSenha", "NovaSenha@123")
        .campo("confirmacaoSenha", "NovaSenha@123")
        .enviar();
    page.aguardarTexto("ALTERAR_SENHA");
    assertThat(page.texto()).contains("Marina Costa", "CLI-000001", "PED-2026-001");
  }

  @Test
  void RF0025_RN0027_detalhesHistoricoEVazio() {
    page.detalhe(1);
    assertThat(page.texto()).contains("4 / 5", "PAG-DEMO-001", "TRO-DEMO-001", "CUP-DEMO-001");
    page.clicar("[data-testid='transacao-3'] summary");
    assertThat(page.texto()).contains("Pagamento fictício do pedido PED-2026-001");
    page.detalhe(2);
    assertThat(page.texto()).contains("Nenhuma transação", "1 / 5").doesNotContain("PED-2026-001");
  }

  @Test
  void RF0027_RN0025_bandeiraRetiradaEnquantoFormularioAberto() {
    page.detalhe(1);
    var f = new CartaoComponente(driver).preencher();
    jdbc.execute("delete from bandeira where id=1");
    f.enviar();
    f.erro();
    assertThat(page.texto()).contains("Selecione uma bandeira cadastrada");
    assertThat(jdbc.queryForObject("select count(*) from cartao", Integer.class)).isZero();
  }

  @Test
  void RF0022_dadosInvalidosELimites() {
    var f = page.editar(1);
    f.campo("nome", "x".repeat(151)).enviar();
    f.erro();
    assertThat(jdbc.queryForObject("select nome from cliente where id=1", String.class))
        .isEqualTo("Marina Costa");
    f.campo("nome", "Marina Costa").campo("cpf", "123").enviar();
    f.erro();
  }

  @Test
  void RF0024_nascimento() {
    page.abrir("", "tabela-clientes");
    page.filtrar("nascimento", "1992-05-18");
    assertThat(page.texto()).contains("Marina Costa").doesNotContain("Cliente Inativo");
  }

  @Test
  void RF0021_nascimentoFuturo() {
    var f = page.cadastro();
    f.campo("nascimento", "2099-01-01").enviar();
    f.erro();
    assertThat(jdbc.queryForObject("select count(*) from cliente", Integer.class)).isEqualTo(2);
  }

  @ParameterizedTest
  @CsvSource({"0,1", "199.99,1", "200,2", "400,3", "600,4", "800,5", "1000,5"})
  void RF0025_RN0027_faixasPeloNavegador(String total, int esperado) {
    jdbc.execute("update transacao_cliente set valor=0 where tipo='PEDIDO'");
    jdbc.update(
        "update transacao_cliente set valor=? where codigo='PED-2026-001'",
        new java.math.BigDecimal(total));
    page.detalhe(1);
    assertThat(page.texto()).contains(esperado + " / 5");
  }

  @Test
  void RF0024_interfaceResponsiva() {
    page.abrir("", "tabela-clientes");
    driver.manage().window().setSize(new org.openqa.selenium.Dimension(390, 844));
    assertThat(
            (Boolean)
                ((JavascriptExecutor) driver)
                    .executeScript(
                        "return document.documentElement.scrollWidth <= window.innerWidth+1"))
        .isTrue();
    assertThat(page.esperar("tabela-clientes").isDisplayed()).isTrue();
  }

  @AfterAll
  void encerrarNavegador() {
    if (driver != null) driver.quit();
  }
}
