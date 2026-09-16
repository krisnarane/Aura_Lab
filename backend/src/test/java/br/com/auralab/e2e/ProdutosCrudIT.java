package br.com.auralab.e2e;

import static org.assertj.core.api.Assertions.*;

import br.com.auralab.PostgresTest;
import br.com.auralab.e2e.pages.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProdutosCrudIT extends PostgresTest {
  @LocalServerPort int port;
  WebDriver driver;
  ProdutosAdminPage page;

  @BeforeEach
  void navegador() {
    if (driver != null) {
      driver.get("http://localhost:" + port + "/admin/produtos");
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
    page = new ProdutosAdminPage(driver, "http://localhost:" + port);
  }

  @AfterEach
  void capturar(TestInfo info) throws Exception {
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

  private void preencherMotivo(String descricaoCategoria, String justificativa) {
    var f = page.formulario("form-motivo");
    f.selecionar("categoria", descricaoCategoria);
    f.campo("justificativa", justificativa);
    f.enviar();
  }

  @Test
  void RF0012_RN0015_RNF0012_inativacaoRegistraMotivoEAuditoria() {
    page.abrir("", "tabela-produtos");
    assertThat(page.linha(2)).contains("PRD-000002").contains("ATIVO").contains("VISÍVEL");
    page.clicar("[data-inativar-produto='2']");
    page.esperar("form-motivo");
    preencherMotivo(
        "Produto descontinuado sem previsão de reposição", "Linha sai do catálogo este mês.");
    page.esperar("tabela-produtos");
    assertThat(page.linha(2)).contains("INATIVO").contains("OCULTO");
    assertThat(jdbc.queryForObject("select ativo from produto where id=2", Boolean.class))
        .isFalse();
    assertThat(jdbc.queryForObject("select visivel from produto where id=2", Boolean.class))
        .isFalse();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auditoria_produto where operacao='INATIVAR_PRODUTO'",
                Integer.class))
        .isEqualTo(1);
    String depois =
        jdbc.queryForObject(
            "select dados_novos::text from auditoria_produto where operacao='INATIVAR_PRODUTO'",
            String.class);
    assertThat(depois).contains("DESCONTINUADO", "Linha sai do catálogo");
  }

  @Test
  void RF0016_RN0017_RNF0012_ativacaoCancelarConfirmarRepetir() {
    page.abrir("", "tabela-produtos");
    assertThat(page.linha(3)).contains("INATIVO").contains("OCULTO");
    page.clicar("[data-ativar-produto='3']");
    page.esperar("form-motivo");
    page.clicar("[data-testid='cancelar-motivo']");
    page.esperar("tabela-produtos");
    assertThat(jdbc.queryForObject("select ativo from produto where id=3", Boolean.class))
        .isFalse();
    page.clicar("[data-ativar-produto='3']");
    page.esperar("form-motivo");
    preencherMotivo(
        "Produto retorna ao mercado após período indisponível",
        "Novo lote chegou ao centro de distribuição.");
    page.esperar("tabela-produtos");
    assertThat(page.linha(3)).contains("ATIVO").contains("OCULTO");
    assertThat(jdbc.queryForObject("select ativo from produto where id=3", Boolean.class))
        .isTrue();
    assertThat(jdbc.queryForObject("select visivel from produto where id=3", Boolean.class))
        .isFalse();
    assertThat(
            jdbc.queryForObject(
                "select categoria_ativacao from produto where id=3", String.class))
        .isEqualTo("RETORNO_AO_MERCADO");
    page.abrir("?motivo=3&acao=ativar", "form-motivo");
    preencherMotivo(
        "Produto retorna ao mercado após período indisponível", "Tentativa repetida.");
    page.aguardarTexto("já está ativo");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auditoria_produto where operacao='ATIVAR_PRODUTO'",
                Integer.class))
        .isEqualTo(1);
    String depois =
        jdbc.queryForObject(
            "select dados_novos::text from auditoria_produto where operacao='ATIVAR_PRODUTO'",
            String.class);
    assertThat(depois).contains("RETORNO_AO_MERCADO", "Novo lote chegou");
  }

  @Test
  void RN0017_justificativaObrigatoriaNaoAtiva() {
    page.abrir("?motivo=3&acao=ativar", "form-motivo");
    var f = page.formulario("form-motivo");
    f.selecionar("categoria", "Produto retorna ao mercado após período indisponível");
    f.enviar();
    f.erro();
    assertThat(jdbc.queryForObject("select ativo from produto where id=3", Boolean.class))
        .isFalse();
    assertThat(
            jdbc.queryForObject("select count(*) from auditoria_produto", Integer.class))
        .isZero();
  }

  @Test
  void RF0011_RNF0021_cadastroGeraCodigoEApareceNaLista() {
    page.abrir("?new=1", "form-produto");
    var f = page.formulario("form-produto");
    f.campo("nome", "Sabonete Líquido Lavanda")
        .campo("marca", "Aura Body")
        .campo("preco", "39.9")
        .campo("estoque", "50");
    f.enviar();
    page.esperar("tabela-produtos");
    assertThat(page.texto()).contains("Sabonete Líquido Lavanda", "PRD-000004");
    assertThat(
            jdbc.queryForObject(
                "select codigo from produto where nome='Sabonete Líquido Lavanda'",
                String.class))
        .isEqualTo("PRD-000004");
  }
}
