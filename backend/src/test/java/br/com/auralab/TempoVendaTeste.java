package br.com.auralab;

import java.time.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

@TestConfiguration
public class TempoVendaTeste {
  @Bean
  @Primary
  public Relogio relogio() {
    return new Relogio();
  }

  public static class Relogio extends Clock {
    private volatile Instant instante = Instant.parse("2026-10-12T12:00:00Z");

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId z) {
      return this;
    }

    public Instant instant() {
      return instante;
    }

    public void reiniciar() {
      instante = Instant.parse("2026-10-12T12:00:00Z");
    }

    public void avancar(Duration d) {
      instante = instante.plus(d);
    }
  }
}
