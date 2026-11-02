package br.com.auralab.ia.service;

public class IaIndisponivelException extends RuntimeException {
  public IaIndisponivelException() {
    super("A consultora está indisponível. Sua pergunta não foi enviada; tente novamente.");
  }
}
