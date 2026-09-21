package br.com.auralab.clientes.api.dto;

/** Saída de cartão com bandeira e últimos quatro dígitos — nunca o PAN (RN0024). */
public record CartaoView(
    Long id,
    Long bandeiraId,
    String bandeira,
    String titular,
    String ultimosQuatro,
    boolean preferencial,
    boolean ativo) {}
