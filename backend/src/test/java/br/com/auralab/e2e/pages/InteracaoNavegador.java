package br.com.auralab.e2e.pages;

import java.time.Duration;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Wait for scrolling and layout before performing a native WebDriver click. */
public final class InteracaoNavegador {
  private InteracaoNavegador() {}

  public static void clicar(WebDriver driver, WebElement elemento) {
    var js = (JavascriptExecutor) driver;
    js.executeScript("arguments[0].scrollIntoView({block:'center',behavior:'instant'})", elemento);
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(
            d ->
                elemento.isDisplayed()
                    && elemento.isEnabled()
                    && Boolean.TRUE.equals(
                        js.executeScript(
                            "const e=arguments[0],r=e.getBoundingClientRect(),x=r.left+r.width/2,y=r.top+r.height/2;"
                                + "if(x<0||y<0||x>=innerWidth||y>=innerHeight)return false;"
                                + "const hit=document.elementFromPoint(x,y);return hit===e||e.contains(hit);",
                            elemento)));
    elemento.click();
  }
}
