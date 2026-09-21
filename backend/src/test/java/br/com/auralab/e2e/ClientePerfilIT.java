package br.com.auralab.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.auralab.PostgresTest;
import br.com.auralab.e2e.pages.ClientePerfilPage;
import br.com.auralab.e2e.pages.FormularioComponente;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientePerfilIT extends PostgresTest {
  @LocalServerPort int port;
  WebDriver driver;
  ClientePerfilPage page;

  @BeforeEach
  void navegador() {
    if (driver == null) {
      var options = new ChromeOptions();
      if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true")))
        options.addArguments("--headless=new");
      options.addArguments("--window-size=1440,1200", "--lang=pt-BR");
      driver = new ChromeDriver(options);
      page = new ClientePerfilPage(driver, "http://localhost:" + port);
    }
    driver.manage().deleteAllCookies();
    driver.manage().window().setSize(new Dimension(1440, 1200));
    driver.get("http://localhost:" + port + "/");
    ((org.openqa.selenium.JavascriptExecutor) driver)
        .executeScript("localStorage.clear();sessionStorage.clear();");
    driver.get("http://localhost:" + port + "/perfil/dados");
    page.esperarSelecao();
  }

  @AfterEach
  void evidencia(TestInfo info) throws Exception {
    if (driver != null) {
      Files.createDirectories(Path.of("target/screenshots"));
      String nome =
          (info.getTestMethod().orElseThrow().getName() + "-" + info.getDisplayName())
              .replaceAll("[^a-zA-Z0-9_-]", "_");
      Files.write(
          Path.of("target/screenshots", nome + ".png"),
          ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }
  }

  @Test
  void RF0022_RNF0012_perfilEscolheClienteAtivoEEditaDados() {
    assertThat(page.texto()).contains("Marina Costa").doesNotContain("Cliente Inativo");
    page.selecionar(1);
    String codigo = jdbc.queryForObject("select codigo from cliente where id=1", String.class);

    var formulario = page.formulario("form-cliente");
    formulario.campo("nome", "Marina Perfil").enviar();

    assertThat(jdbc.queryForObject("select nome from cliente where id=1", String.class))
        .isEqualTo("Marina Perfil");
    assertThat(formulario.valor("nome")).isEqualTo("Marina Perfil");
    assertThat(jdbc.queryForObject("select codigo from cliente where id=1", String.class))
        .isEqualTo(codigo);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auditoria_cliente where operacao='ALTERAR_CLIENTE'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void RF0028_RNF0033_perfilAlteraSenhaSemExporHash() {
    page.selecionar(1);
    page.abrir("/perfil/seguranca", "form-senha");
    String hashAntes = jdbc.queryForObject("select senha_hash from cliente where id=1", String.class);

    page.formulario("form-senha")
        .campo("novaSenha", "NovaSenha@123")
        .campo("confirmacaoSenha", "NovaSenha@123")
        .enviar();

    String hashDepois = jdbc.queryForObject("select senha_hash from cliente where id=1", String.class);
    assertThat(hashDepois).isNotEqualTo(hashAntes);
    assertThat(new BCryptPasswordEncoder().matches("NovaSenha@123", hashDepois)).isTrue();
    assertThat(page.texto()).doesNotContain(hashDepois, "NovaSenha@123");
  }

  @Test
  void RF0022_perfilRecusaDadosInvalidosSemPersistir() {
    page.selecionar(1);
    String nomeAntes = jdbc.queryForObject("select nome from cliente where id=1", String.class);
    var formulario = page.formulario("form-cliente");
    formulario.campo("nome", "x".repeat(151)).enviar();
    formulario.erro();

    assertThat(jdbc.queryForObject("select nome from cliente where id=1", String.class))
        .isEqualTo(nomeAntes);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auditoria_cliente where operacao='ALTERAR_CLIENTE'",
                Integer.class))
        .isZero();
  }

  @Test
  void RF0028_RNF0031_RNF0032_perfilRecusaSenhaFracaOuDiferente() {
    page.selecionar(1);
    page.abrir("/perfil/seguranca", "form-senha");
    String hashAntes = jdbc.queryForObject("select senha_hash from cliente where id=1", String.class);
    var formulario = page.formulario("form-senha");
    formulario.campo("novaSenha", "fraca").campo("confirmacaoSenha", "diferente").enviar();
    formulario.erro();

    assertThat(jdbc.queryForObject("select senha_hash from cliente where id=1", String.class))
        .isEqualTo(hashAntes);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auditoria_cliente where operacao='ALTERAR_SENHA'",
                Integer.class))
        .isZero();
  }

  @Test
  void RF0026_RNF0034_perfilAdicionaEEditaEndereco() {
    page.selecionar(1);
    page.abrir("/perfil/enderecos", "form-endereco");
    new FormularioComponente(driver, "form-endereco")
        .campo("endereco_apelido", "Trabalho")
        .campo("endereco_tipoResidencia", "Comercial")
        .campo("endereco_tipoLogradouro", "Avenida")
        .campo("endereco_logradouro", "Paulista")
        .campo("endereco_numero", "1000")
        .campo("endereco_bairro", "Bela Vista")
        .campo("endereco_cep", "01311000")
        .campo("endereco_cidade", "São Paulo")
        .campo("endereco_estado", "SP")
        .campo("endereco_pais", "Brasil")
        .enviar();
    page.aguardarTexto("Trabalho");

    Long enderecoId =
        jdbc.queryForObject("select id from endereco where cliente_id=1 and apelido='Trabalho'", Long.class);
    page.abrir("/perfil/enderecos?edit=" + enderecoId, "form-endereco");
    page.formulario("form-endereco").campo("endereco_complemento", "Sala 5").enviar();
    page.aguardarTexto("Sala 5");

    assertThat(
            jdbc.queryForObject("select complemento from endereco where id=?", String.class, enderecoId))
        .isEqualTo("Sala 5");
  }

  @Test
  void RN0023_perfilRecusaEnderecoInvalidoSemPersistir() {
    page.selecionar(1);
    page.abrir("/perfil/enderecos", "form-endereco");
    int enderecosAntes = jdbc.queryForObject("select count(*) from endereco", Integer.class);
    var formulario = page.formulario("form-endereco");
    formulario
        .campo("endereco_apelido", "Inválido")
        .campo("endereco_tipoResidencia", "Casa")
        .campo("endereco_tipoLogradouro", "Rua")
        .campo("endereco_logradouro", "Teste")
        .campo("endereco_numero", "10")
        .campo("endereco_bairro", "Centro")
        .campo("endereco_cep", "123")
        .campo("endereco_cidade", "São Paulo")
        .campo("endereco_estado", "SP")
        .campo("endereco_pais", "Brasil")
        .enviar();
    formulario.erro();

    assertThat(jdbc.queryForObject("select count(*) from endereco", Integer.class))
        .isEqualTo(enderecosAntes);
  }

  @Test
  void RF0027_RN0024_RN0025_perfilAdicionaCartaoMascarado() {
    page.selecionar(1);
    page.abrir("/perfil/cartoes", "form-cartao");
    page.formulario("form-cartao")
        .campo("numero", "4111111111111111")
        .campo("titular", "MARINA PERFIL")
        .selecionar("bandeiraId", "Visa")
        .campo("codigoSeguranca", "123")
        .enviar();
    page.aguardarTexto("Visa final 1111");

    assertThat(page.texto()).doesNotContain("4111111111111111", "123");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from cartao where cliente_id=1 and ultimos_quatro='1111'",
                Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from cartao", Integer.class)).isEqualTo(1);
  }

  @Test
  void RF0027_RN0024_perfilRecusaCartaoInvalidoSemPersistir() {
    page.selecionar(1);
    page.abrir("/perfil/cartoes", "form-cartao");
    var formulario = page.formulario("form-cartao");
    formulario
        .campo("numero", "123")
        .campo("titular", "MARINA PERFIL")
        .selecionar("bandeiraId", "Visa")
        .campo("codigoSeguranca", "1")
        .enviar();
    formulario.erro();

    assertThat(jdbc.queryForObject("select count(*) from cartao", Integer.class)).isZero();
  }

  @AfterAll
  void encerrarNavegador() {
    if (driver != null) driver.quit();
  }
}