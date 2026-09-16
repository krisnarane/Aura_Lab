package br.com.auralab.e2e.pages;

import java.time.Duration;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

public class ClientesAdminPage {
  private final WebDriver driver;
  private final String base;
  private final WebDriverWait wait;

  public ClientesAdminPage(WebDriver d, String base) {
    this.driver = d;
    this.base = base;
    wait = new WebDriverWait(d, Duration.ofSeconds(15));
  }

  public ClientesAdminPage abrir(String sufixo, String pronto) {
    driver.get(base + "/admin/clientes" + sufixo);
    esperar(pronto);
    return this;
  }

  public WebElement esperar(String id) {
    return wait.until(
        ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("[data-testid='" + id + "']")));
  }

  public FormularioComponente cadastro() {
    abrir("?new=1", "form-cliente");
    var f = new FormularioComponente(driver, "form-cliente");
    f.campo("nome", "Cliente Teste")
        .campo("cpf", "11144477735")
        .campo("email", "novo@example.com")
        .campo("genero", "Feminino")
        .campo("nascimento", "01/01/1990")
        .campo("telefoneDdd", "11")
        .campo("telefoneNumero", "988887777")
        .campo("senha", "Senha@123")
        .campo("confirmacaoSenha", "Senha@123");
    String[][] dados = {
      {"apelido", "Casa"},
      {"tipoResidencia", "Casa"},
      {"tipoLogradouro", "Rua"},
      {"logradouro", "Flores"},
      {"numero", "10"},
      {"bairro", "Centro"},
      {"cep", "01001000"},
      {"cidade", "São Paulo"},
      {"estado", "SP"},
      {"pais", "Brasil"}
    };
    for (var c : dados) f.campo("endereco_" + c[0], c[1]);
    return f;
  }

  public FormularioComponente editar(long id) {
    abrir("?edit=" + id, "form-cliente");
    return new FormularioComponente(driver, "form-cliente");
  }

  public ClientesAdminPage detalhe(long id) {
    return abrir("/" + id, "form-endereco");
  }

  public String texto() {
    return driver.findElement(By.tagName("body")).getText();
  }

  public void clicar(String css) {
    var e = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(css)));
    InteracaoNavegador.clicar(driver, e);
  }

  public void aguardarTexto(String texto) {
    wait.until(d -> texto().contains(texto));
  }

  public void aguardarAtualizacao(WebElement velho) {
    wait.until(ExpectedConditions.stalenessOf(velho));
    esperar("form-endereco");
  }

  public void filtrar(String campo, String valor) {
    var f = new FormularioComponente(driver, "filtros-clientes");
    f.campo(campo, valor);
    var tabela = esperar("tabela-clientes");
    clicar("[data-testid='filtros-clientes'] button");
    wait.until(ExpectedConditions.stalenessOf(tabela));
    esperar("tabela-clientes");
  }
}
