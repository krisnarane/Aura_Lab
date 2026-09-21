package br.com.auralab.e2e.pages;

import java.time.Duration;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ClientePerfilPage {
  private final WebDriver driver;
  private final String base;
  private final WebDriverWait wait;

  public ClientePerfilPage(WebDriver driver, String base) {
    this.driver = driver;
    this.base = base;
    wait = new WebDriverWait(driver, Duration.ofSeconds(15));
  }

  public ClientePerfilPage abrir(String rota, String pronto) {
    driver.get(base + rota);
    esperar(pronto);
    return this;
  }

  public WebElement esperar(String testId) {
    return wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("[data-testid='" + testId + "']")));
  }

  public ClientePerfilPage selecionar(long id) {
    var cliente =
        wait.until(
            ExpectedConditions.elementToBeClickable(
                By.cssSelector("[data-entrar-cliente='" + id + "']")));
    InteracaoNavegador.clicar(driver, cliente);
    esperar("form-cliente");
    return this;
  }

  public void esperarSelecao() {
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("[data-entrar-cliente='1']")));
  }

  public FormularioComponente formulario(String testId) {
    return new FormularioComponente(driver, testId);
  }

  public void clicar(String css) {
    var elemento = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(css)));
    InteracaoNavegador.clicar(driver, elemento);
  }

  public void aguardarTexto(String texto) {
    wait.until(d -> texto().contains(texto));
  }

  public String texto() {
    return driver.findElement(By.tagName("body")).getText();
  }
}