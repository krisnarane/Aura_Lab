package br.com.auralab.e2e;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.auralab.PostgresTest;
import br.com.auralab.TempoVendaTeste;
import br.com.auralab.vendas.api.dto.*;
import br.com.auralab.vendas.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TempoVendaTeste.class)
class AnaliseVendasIT extends PostgresTest {
  @LocalServerPort int port;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired VendaService vendas;
  @Autowired CicloPedidoService ciclo;
  @Autowired TempoVendaTeste.Relogio relogio;
  WebDriver driver;

  @AfterEach void fechar() { if (driver != null) driver.quit(); }

  private long comprar(long produto) throws Exception {
    return comprar(produto, null);
  }

  /** {@code resultadoOperadora} nulo usa o padrão APROVADO da operadora simulada. */
  private long comprar(long produto, String resultadoOperadora) throws Exception {
    mvc.perform(post("/api/v1/clientes/1/carrinho/itens").contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(Map.of("produtoId", produto, "quantidade", 1))))
        .andExpect(status().isCreated());
    long cartao = jdbc.queryForObject("insert into cartao(cliente_id,bandeira_id,titular,ultimos_quatro,preferencial,ativo) "
        + "values(1,1,'MARINA COSTA','1111',false,true) returning id", Long.class);
    if (resultadoOperadora != null)
      jdbc.update("insert into resultado_operadora_cartao(cartao_id,resultado) values (?,?)",
          cartao, resultadoOperadora);
    var resumo = json.readTree(mvc.perform(post("/api/v1/clientes/1/carrinho/resumo")
        .contentType(MediaType.APPLICATION_JSON).content("{\"enderecoId\":1}"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    long pedido = vendas.finalizar(1L, new CheckoutInput(1L, null, false, null, List.of(),
        List.of(new ParcelaCartaoInput(cartao, null, false, resumo.path("total").decimalValue())))).id();
    ciclo.validar(pedido);
    return pedido;
  }

  private void entregar(long pedido) throws Exception {
    mvc.perform(post("/api/v1/admin/pedidos/" + pedido + "/despacho").contentType(MediaType.APPLICATION_JSON)
        .content("{}")).andExpect(status().isOk());
    mvc.perform(post("/api/v1/admin/pedidos/" + pedido + "/entrega").contentType(MediaType.APPLICATION_JSON)
        .content("{}")).andExpect(status().isOk());
  }

  private WebElement esperar(String seletor) {
    return new WebDriverWait(driver, Duration.ofSeconds(15))
        .until(d -> d.findElement(By.cssSelector(seletor)));
  }

  private String moeda(long pedido) {
    BigDecimal total = jdbc.queryForObject("select total from pedido where id=?", BigDecimal.class, pedido);
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
        .format(total).replaceAll("[\\s\\u00a0]", "");
  }

  private void consultar(String inicio, String fim, String... categorias) {
    var form = esperar("#analise-form");
    var comeco = form.findElement(By.name("inicio"));
    var termino = form.findElement(By.name("fim"));
    ((JavascriptExecutor) driver).executeScript("arguments[0].value=arguments[2];arguments[1].value=arguments[3]", comeco, termino, inicio, fim);
    // Caixas de seleção das categorias controladas explicitamente para a demonstração.
    ((JavascriptExecutor) driver).executeScript(
        "for(const c of arguments[0].querySelectorAll('input[name=\"categorias\"]'))c.checked=arguments[1].includes(c.value)",
        form, Arrays.asList(categorias));
    var anterior = driver.findElements(By.cssSelector("[data-testid='tabela-vendas']"));
    form.findElement(By.cssSelector("button[type=submit]")).click();
    if (!anterior.isEmpty()) new WebDriverWait(driver, Duration.ofSeconds(15))
        .until(ExpectedConditions.stalenessOf(anterior.get(0)));
    esperar("[data-testid='tabela-vendas']");
  }

  @Test void RF0055_RNF0043_roteiroSeteCenariosDoGrafico() throws Exception {
    relogio.reiniciar();
    jdbc.update("delete from produto_categoria where produto_id in (1,2)");
    jdbc.update("insert into produto_categoria values(1,'FACE'),(2,'SKINCARE')");
    long outubro = comprar(1);
    entregar(outubro);
    relogio.avancar(Duration.ofDays(31));
    // RN0074: a compra reprovada pela operadora não é contabilizada no gráfico.
    comprar(2, "REPROVADO");
    var options = new ChromeOptions();
    if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true"))) options.addArguments("--headless=new");
    options.addArguments("--window-size=1440,1200", "--lang=pt-BR");
    driver = new ChromeDriver(options);
    driver.get("http://localhost:" + port + "/admin/dashboard");
    consultar("2026-10-01", "2026-11-30", "FACE");
    assertThat(esperar("[data-testid='grafico-vendas']").getAttribute("role")).isEqualTo("img");
    assertThat(esperar("[data-testid='ponto-FACE-2026-10']").getAttribute("aria-label")).contains("10/2026", "R$");
    assertThat(esperar("[data-testid='valor-FACE-2026-10']").getText().replaceAll("[\\s\\u00a0]", ""))
        .isEqualTo(moeda(outubro));
    consultar("2026-10-01", "2026-11-30", "FACE", "SKINCARE");
    assertThat(esperar("[data-testid='serie-SKINCARE']")).isNotNull();
    assertThat(esperar("[data-testid='valor-SKINCARE-2026-11']").getText()).contains("0,00");
    // RNF0043: uma linha por categoria selecionada. RNF0045: legenda com nome e cor, embaixo.
    assertThat(driver.findElements(By.cssSelector("[data-testid='serie-FACE'] polyline"))).isNotEmpty();
    assertThat(driver.findElements(By.cssSelector("[data-testid='serie-SKINCARE'] polyline"))).isNotEmpty();
    var legenda = esperar("[data-testid='legenda-vendas']");
    assertThat(legenda.getText()).contains("Face", "Skincare");
    assertThat(legenda.getLocation().getY())
        .as("RNF0045: legenda abaixo do gráfico")
        .isGreaterThan(esperar("[data-testid='grafico-vendas']").getLocation().getY());
    // RF0055: antes da entrega, a compra APROVADA ainda não entra no histórico.
    long novembro = comprar(2);
    consultar("2026-10-01", "2026-11-30", "FACE", "SKINCARE");
    assertThat(esperar("[data-testid='valor-SKINCARE-2026-11']").getText()).contains("0,00");
    // Após a transição para ENTREGUE, a consulta seguinte passa a contabilizar a compra.
    entregar(novembro);
    consultar("2026-10-01", "2026-11-30", "FACE", "SKINCARE");
    assertThat(esperar("[data-testid='valor-SKINCARE-2026-11']").getText().replaceAll("[\\s\\u00a0]", ""))
        .isEqualTo(moeda(novembro));
    consultar("2026-10-01", "2026-10-31", "FACE", "SKINCARE");
    assertThat(driver.findElements(By.cssSelector("[data-testid='valor-SKINCARE-2026-11']"))).isEmpty();
    driver.findElement(By.cssSelector("[data-perfil='CLIENTE_DEMO']")).click();
    assertThat(esperar("[data-testid='acesso-negado']").getText()).contains("Acesso negado");
    assertThat(driver.findElements(By.cssSelector("[data-testid='grafico-vendas']"))).isEmpty();
    driver.findElement(By.cssSelector("[data-perfil='ADMIN_DEMO']")).click();
    esperar("[data-testid='grafico-vendas']");
  }
}
