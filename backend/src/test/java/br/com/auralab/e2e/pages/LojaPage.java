package br.com.auralab.e2e.pages;

import java.time.Duration;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Page Object da jornada principal integrada: catálogo, sacola e checkout contra a API real. */
public class LojaPage {
  private final WebDriver driver;
  private final String base;
  private final WebDriverWait wait;

  public LojaPage(WebDriver driver, String base) {
    this.driver = driver;
    this.base = base;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
  }

  public LojaPage abrir(String rota) {
    driver.get(base + rota);
    return this;
  }

  public void esperarSelecao() {
    esperar("[data-entrar-cliente='1'],[data-testid='ir-carrinho']");
  }

  public LojaPage entrar(long clienteId) {
    var selecao = driver.findElements(By.cssSelector("[data-entrar-cliente='" + clienteId + "']"));
    if (!selecao.isEmpty()) clicar("[data-entrar-cliente='" + clienteId + "']");
    esperar("[data-testid='ir-carrinho']");
    return this;
  }

  public LojaPage adicionar(long produtoId, int quantidade) {
    setValor("[data-testid='qtd-" + produtoId + "']", String.valueOf(quantidade));
    clicarRecarregando("[data-testid='add-" + produtoId + "']", "[data-testid='ir-carrinho']");
    return this;
  }

  public void esperarBadge(int totalUnidades) {
    wait.until(
        d ->
            texto("[data-testid='ir-carrinho'] b")
                .equals(String.valueOf(totalUnidades)));
  }

  /** Tenta adicionar e espera a notificação de erro (ex.: estoque insuficiente, RN0031). */
  public LojaPage adicionarEsperandoErro(long produtoId, int quantidade) {
    setValor("[data-testid='qtd-" + produtoId + "']", String.valueOf(quantidade));
    clicar("[data-testid='add-" + produtoId + "']");
    wait.until(
        ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".aura-toast.is-error")));
    return this;
  }

  public LojaPage abrirCarrinho() {
    clicar("[data-testid='ir-carrinho']");
    esperar("[data-testid='carrinho-subtotal'],[data-testid='carrinho-vazio']");
    return this;
  }

  public LojaPage atualizarItem(long itemId, int quantidade) {
    setValor("[data-testid='qtd-item-" + itemId + "']", String.valueOf(quantidade));
    clicarRecarregando(
        "[data-testid='atualizar-" + itemId + "']", "[data-testid='carrinho-subtotal']");
    return this;
  }

  public LojaPage removerItem(long itemId) {
    clicarRecarregando(
        "[data-testid='remover-" + itemId + "']",
        "[data-testid='carrinho-subtotal'],[data-testid='carrinho-vazio']");
    return this;
  }

  public LojaPage abrirCheckout() {
    if (driver.findElements(By.cssSelector("[data-testid='ir-checkout']")).isEmpty()) {
      abrirCarrinho();
    }
    clicar("[data-testid='ir-checkout']");
    esperar("[data-testid='form-checkout']");
    return this;
  }

  public LojaPage escolherEndereco(long enderecoId) {
    clicar("[data-testid='endereco-" + enderecoId + "']");
    return this;
  }

  public LojaPage escolherNovoEndereco() {
    clicar("[data-testid='endereco-novo']");
    return this;
  }

  public LojaPage preencherNovoEndereco(String apelido, String estado) {
    campo("endereco_apelido", apelido);
    campo("endereco_tipoResidencia", "Comercial");
    campo("endereco_tipoLogradouro", "Avenida");
    campo("endereco_logradouro", "Paulista");
    campo("endereco_numero", "1000");
    campo("endereco_bairro", "Bela Vista");
    campo("endereco_cep", "01311000");
    campo("endereco_cidade", "São Paulo");
    campo("endereco_estado", estado);
    campo("endereco_pais", "Brasil");
    return this;
  }

  public LojaPage salvarEnderecoNoPerfil() {
    marcar("[data-testid='incorporar-endereco']");
    return this;
  }

  public LojaPage promocional(String codigo) {
    setValor("[data-testid='cupom-promocional']", codigo);
    return this;
  }

  public LojaPage marcarCupom(long cupomId) {
    marcar("[data-testid='cupom-" + cupomId + "']");
    return this;
  }

  public LojaPage valorCartao(long cartaoId, String valor) {
    setValor("[data-testid='valor-cartao-" + cartaoId + "']", valor);
    return this;
  }

  public LojaPage abrirNovoCartao() {
    clicar("[data-testid='novo-cartao']");
    esperar("[data-testid='cartao-numero']");
    return this;
  }

  public LojaPage preencherNovoCartao(
      String numero, String titular, String bandeira, String cvv, String valor) {
    campo("cartao_numero", numero);
    campo("cartao_titular", titular);
    new Select(driver.findElement(By.name("cartao_bandeiraId"))).selectByVisibleText(bandeira);
    campo("cartao_codigoSeguranca", cvv);
    setValor("[data-testid='valor-cartao-novo']", valor);
    return this;
  }

  public LojaPage salvarCartaoNoPerfil() {
    marcar("[data-testid='incorporar-cartao']");
    return this;
  }

  public LojaPage calcularResumo() {
    clicar("[data-testid='calcular-resumo']");
    return this;
  }

  /** Clica em finalizar e aguarda a confirmação (sucesso) ou a mensagem de erro. */
  public LojaPage finalizar() {
    clicar("[data-testid='finalizar-compra']");
    wait.until(
        d ->
            !d.findElements(By.cssSelector("[data-testid='pedido-confirmado']")).isEmpty()
                || !d.findElements(By.cssSelector("[data-testid='erro-api']")).isEmpty());
    return this;
  }

  public String statusPedido() {
    return esperar("[data-testid='pedido-status']").getText();
  }

  public boolean temErro() {
    return !driver.findElements(By.cssSelector("[data-testid='erro-api']")).isEmpty();
  }

  public String erro() {
    return esperar("[data-testid='erro-api']").getText();
  }

  public String texto() {
    return driver.findElement(By.tagName("body")).getText();
  }

  // ---- infraestrutura ----

  private WebElement esperar(String css) {
    return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(css)));
  }

  private String texto(String css) {
    var els = driver.findElements(By.cssSelector(css));
    return els.isEmpty() ? "" : els.get(0).getText();
  }

  private void clicar(String css) {
    InteracaoNavegador.clicar(
        driver, wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(css))));
  }

  private void clicarRecarregando(String css, String prontoCss) {
    WebElement el = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(css)));
    InteracaoNavegador.clicar(driver, el);
    wait.until(ExpectedConditions.stalenessOf(el));
    esperar(prontoCss);
  }

  private void setValor(String css, String valor) {
    WebElement el = esperar(css);
    el.clear();
    if (!valor.isEmpty()) el.sendKeys(valor);
  }

  private void campo(String name, String valor) {
    WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name(name)));
    el.clear();
    if (!valor.isEmpty()) el.sendKeys(valor);
  }

  private void marcar(String css) {
    WebElement el = esperar(css);
    if (!el.isSelected()) InteracaoNavegador.clicar(driver, el);
  }
}
