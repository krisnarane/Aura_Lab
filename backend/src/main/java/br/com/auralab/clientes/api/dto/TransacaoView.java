package br.com.auralab.clientes.api.dto;

import java.math.BigDecimal;
import java.time.*;

/** Saída de transação demonstrativa vinculada ao cliente (RF0025). */
public record TransacaoView(
    Long id,
    String codigo,
    String tipo,
    String status,
    BigDecimal valor,
    OffsetDateTime ocorridaEm,
    String detalhes) {}
