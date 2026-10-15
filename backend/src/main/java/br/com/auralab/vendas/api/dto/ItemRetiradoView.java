package br.com.auralab.vendas.api.dto;

import java.time.OffsetDateTime;

public record ItemRetiradoView(
    Long produtoId,
    String produtoNome,
    int quantidade,
    String motivo,
    long prazoMinutos,
    OffsetDateTime retiradoEm,
    boolean readicionado) {}
