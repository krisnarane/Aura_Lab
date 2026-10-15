package br.com.auralab.config;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class TempoVendaConfig {
  @Bean
  @ConditionalOnMissingBean(Clock.class)
  Clock relogioVenda() {
    return Clock.systemUTC();
  }
}
