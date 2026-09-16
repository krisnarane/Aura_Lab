package br.com.auralab.clientes.api.dto;

public record CartaoView(
    Long id,
    Long bandeiraId,
    String bandeira,
    String titular,
    String ultimosQuatro,
    boolean preferencial,
    boolean ativo) {}
