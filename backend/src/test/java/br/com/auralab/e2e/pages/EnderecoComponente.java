package br.com.auralab.e2e.pages;

import org.openqa.selenium.WebDriver;

public class EnderecoComponente extends FormularioComponente {
  public EnderecoComponente(WebDriver d) {
    super(d, "form-endereco");
  }

  public EnderecoComponente preencher(String apelido) {
    campo("apelido", apelido);
    campo("tipoResidencia", "Casa");
    campo("tipoLogradouro", "Rua");
    campo("logradouro", "Flores");
    campo("numero", "20");
    campo("bairro", "Centro");
    campo("cep", "01001000");
    campo("cidade", "São Paulo");
    campo("estado", "SP");
    campo("pais", "Brasil");
    campo("complemento", "Apto 2");
    return this;
  }
}
