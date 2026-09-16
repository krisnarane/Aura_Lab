package br.com.auralab.e2e.pages;

import java.time.Duration;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

public class FormularioComponente {
  private final WebDriver driver;
  private final String seletor;

  public FormularioComponente(WebDriver d, String testId) {
    driver = d;
    seletor = "[data-testid='" + testId + "']";
  }

  private WebElement form() {
    return driver.findElement(By.cssSelector(seletor));
  }

  public FormularioComponente campo(String nome, String valor) {
    var c = form().findElement(By.name(nome));
    if ("date".equals(c.getDomAttribute("type")) && valor.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
      ((JavascriptExecutor) driver)
          .executeScript(
              "arguments[0].value=arguments[1];arguments[0].dispatchEvent(new Event('input',{bubbles:true}));arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
              c,
              valor);
      return this;
    }
    c.clear();
    if (!valor.isEmpty()) c.sendKeys(valor);
    return this;
  }

  public FormularioComponente marcar(String nome, boolean valor) {
    var c = form().findElement(By.name(nome));
    if (c.isSelected() != valor) clicar(c);
    return this;
  }

  public FormularioComponente selecionar(String nome, String texto) {
    new Select(form().findElement(By.name(nome))).selectByVisibleText(texto);
    return this;
  }

  public String valor(String nome) {
    return form().findElement(By.name(nome)).getDomProperty("value");
  }

  public void enviar() {
    var atual = form();
    clicar(atual.findElement(By.cssSelector("button")));
    new WebDriverWait(driver, Duration.ofSeconds(15))
        .until(
            d -> {
              try {
                return !atual.findElements(By.cssSelector("[data-form-error]")).isEmpty()
                    || !atual.isDisplayed();
              } catch (StaleElementReferenceException e) {
                return true;
              }
            });
  }

  public void erro() {
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(seletor + " [data-form-error]")));
  }

  private void clicar(WebElement e) {
    InteracaoNavegador.clicar(driver, e);
  }
}
