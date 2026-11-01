package br.com.auralab.produtos.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aura.produtos.agendamento-inativacao-ativo", havingValue = "true", matchIfMissing = true)
public class JobInativacaoProdutos {
  private final InativacaoAutomaticaProdutos inativacao;
  public JobInativacaoProdutos(InativacaoAutomaticaProdutos inativacao) { this.inativacao = inativacao; }
  @Scheduled(fixedDelayString = "${aura.produtos.intervalo-job-ms:60000}")
  public void executar() { inativacao.executar(); }
}
