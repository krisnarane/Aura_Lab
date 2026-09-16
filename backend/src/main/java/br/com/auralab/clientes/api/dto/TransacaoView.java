package br.com.auralab.clientes.api.dto;

import java.math.BigDecimal;
import java.time.*;

public record TransacaoView(
    Long id,
    String codigo,
    String tipo,
    String status,
    BigDecimal valor,
    OffsetDateTime ocorridaEm,
    String detalhes) {}
