package br.com.auralab.clientes.api.dto;

public record ErroView(String codigo, String mensagem, java.util.Map<String, String> campos) {}
