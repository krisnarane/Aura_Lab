package br.com.auralab.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import br.com.auralab.PostgresTest;
import br.com.auralab.ia.service.GeradorIa;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsultoraIT extends PostgresTest {
  @LocalServerPort int port;
  @MockitoBean GeradorIa gerador;
  WebDriver driver;

  @AfterEach
  void fechar() {
    if (driver != null) driver.quit();
  }

  @Test
  void RNF0044_clienteConversaAvaliaEReabreVoto() {
    jdbc.update("insert into ia_modelo(versao,modelo_base,endpoint,job_nome,dataset_sha256,manifesto,ativo) "
        + "values ('selenium-v1','gemini-2.5-flash-lite','endpoint-simulado','job-simulado',repeat('a',64),'{}'::jsonb,true)");
    when(gerador.gerar(anyString(), anyString()))
        .thenReturn("{\"texto\":\"O Creme Facial pode combinar com sua rotina.\",\"produtosIds\":[1]}");
    var options = new ChromeOptions();
    if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true")))
      options.addArguments("--headless=new");
    options.addArguments("--window-size=1440,1000", "--lang=pt-BR");
    driver = new ChromeDriver(options);
    driver.get("http://localhost:" + port + "/");
    ((JavascriptExecutor) driver).executeScript(
        "localStorage.setItem('aura-lab-perfil-api-cliente','1');"
            + "localStorage.setItem('aura-lab-perfil-api-nome','Marina Costa');");
    driver.get("http://localhost:" + port + "/consultora");
    var espera = new WebDriverWait(driver, Duration.ofSeconds(20));
    espera.until(d -> d.findElement(By.id("advisor-message")));
    driver.findElement(By.id("advisor-message")).sendKeys("O que recomenda para mim?");
    driver.findElement(By.cssSelector("#chat-form button")).click();
    espera.until(d -> d.findElements(By.cssSelector("[data-avaliar]")).size() == 2);
    assertThat(driver.findElement(By.id("aura-chat-messages")).getText())
        .contains("Creme Facial", "Modelo selenium-v1");
    driver.findElement(By.cssSelector("[data-avaliar][data-util='true']")).click();
    espera.until(d -> jdbc.queryForObject("select count(*) from ia_avaliacao where util", Integer.class) == 1);
    driver.navigate().refresh();
    espera.until(d -> d.findElements(By.cssSelector("[data-avaliar][data-util='true'].aura-btn")).size() == 1);
    assertThat(jdbc.queryForObject("select count(*) from ia_mensagem", Integer.class)).isEqualTo(2);
  }
}
