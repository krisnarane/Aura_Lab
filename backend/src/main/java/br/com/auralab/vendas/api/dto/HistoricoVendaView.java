package br.com.auralab.vendas.api.dto;

import java.time.OffsetDateTime;

public record HistoricoVendaView(
    OffsetDateTime data, String operacao, String ator, String status) {}
