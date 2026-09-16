package br.com.auralab.e2e.pages;

import java.time.Duration;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

public class ProdutosAdminPage {
  private final WebDriver driver;
  private final String base;
  private final WebDriverWait wait;

  public ProdutosAdminPage(WebDriver d, String base) {
    this.driver = d;
    this.base = base;
    wait = new WebDriverWait(d, Duration.ofSeconds(15));
  }

  public ProdutosAdminPage abrir(String sufixo, String pronto) {
    driver.get(base + "/admin/produtos" + sufixo);
    esperar(pronto);
    return this;
  }

  public WebElement esperar(String id) {
    return wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("[data-testid='" + id + "']")));
  }

  public String texto() {
    return driver.findElement(By.tagName("body")).getText();
  }

  public String linha(long id) {
    esperar("produto-" + id);
    return driver
        .findElement(By.cssSelector("[data-testid='produto-" + id + "']"))
        .getText();
  }

  public FormularioComponente formulario(String testId) {
    return new FormularioComponente(driver, testId);
  }

  public void clicar(String css) {
    var el = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(css)));
    InteracaoNavegador.clicar(driver, el);
  }

  public void aguardarTexto(String texto) {
    wait.until(d -> texto().contains(texto));
  }
}
