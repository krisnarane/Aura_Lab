package br.com.auralab.produtos.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntradaEstoqueInput(Integer quantidade, BigDecimal valorCusto, LocalDate dataEntrada) {}
