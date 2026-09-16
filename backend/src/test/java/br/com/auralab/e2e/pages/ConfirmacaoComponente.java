package br.com.auralab.e2e.pages;

import java.time.Duration;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

public class ConfirmacaoComponente {
  private final WebDriver driver;

  public ConfirmacaoComponente(WebDriver d) {
    driver = d;
  }

  public void responder(boolean confirmar) {
    var a =
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.alertIsPresent());
    if (confirmar) a.accept();
    else a.dismiss();
  }
}
