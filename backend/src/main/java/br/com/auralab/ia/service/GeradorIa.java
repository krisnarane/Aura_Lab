package br.com.auralab.ia.service;

/** Chamada externa isolada para testes sem rede. RNF0044. */
public interface GeradorIa {
  String gerar(String endpoint, String prompt);
}
