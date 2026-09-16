package br.com.auralab.e2e.pages;

import org.openqa.selenium.WebDriver;

public class CartaoComponente extends FormularioComponente {
  public CartaoComponente(WebDriver d) {
    super(d, "form-cartao");
  }

  public CartaoComponente preencher() {
    campo("numero", "4111111111111111");
    campo("titular", "TITULAR TESTE");
    selecionar("bandeiraId", "Visa");
    campo("codigoSeguranca", "123");
    return this;
  }
}
