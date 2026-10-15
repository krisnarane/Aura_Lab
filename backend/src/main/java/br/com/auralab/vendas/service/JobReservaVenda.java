package br.com.auralab.vendas.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "aura.vendas.agendamento-ativo",
    havingValue = "true",
    matchIfMissing = true)
public class JobReservaVenda {
  private final ReservaEstoqueService reservas;

  public JobReservaVenda(ReservaEstoqueService reservas) {
    this.reservas = reservas;
  }

  @Scheduled(fixedDelayString = "${aura.vendas.intervalo-job-ms:1000}")
  public void executar() {
    reservas.processarVencimentos();
  }
}
