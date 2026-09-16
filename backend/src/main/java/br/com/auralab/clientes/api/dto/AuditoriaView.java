package br.com.auralab.clientes.api.dto;

import java.time.*;

public record AuditoriaView(
    Long id,
    OffsetDateTime ocorridaEm,
    String ator,
    String operacao,
    String alteracoes,
    String entidade,
    Long entidadeId,
    java.util.Map<String, Object> dadosAnteriores,
    java.util.Map<String, Object> dadosNovos) {}
