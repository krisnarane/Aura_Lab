package br.com.auralab.produtos.api.dto;

import java.math.BigDecimal;

public record GrupoPrecificacaoView(String codigo, String descricao, BigDecimal percentualMargem) {}
