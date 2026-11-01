package br.com.auralab.produtos.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record EntradaEstoqueView(Long id, int quantidade, BigDecimal valorCusto, LocalDate dataEntrada,
    BigDecimal precoCalculado, OffsetDateTime registradaEm) {}
