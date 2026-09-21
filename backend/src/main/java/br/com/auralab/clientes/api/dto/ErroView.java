package br.com.auralab.clientes.api.dto;

/** Contrato de erro padronizado da API: {codigo, mensagem, campos}. */
public record ErroView(String codigo, String mensagem, java.util.Map<String, String> campos) {}
