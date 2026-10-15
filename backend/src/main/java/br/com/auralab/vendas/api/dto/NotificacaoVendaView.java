package br.com.auralab.vendas.api.dto;

import java.time.OffsetDateTime;

public record NotificacaoVendaView(
    Long id, String tipo, String mensagem, OffsetDateTime criadaEm, boolean lida) {}
