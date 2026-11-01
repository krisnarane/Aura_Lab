package br.com.auralab.produtos.api.dto;

import java.math.BigDecimal;

/** RF0015: filtros opcionais e combináveis da consulta administrativa. */
public record FiltroProduto(String codigo, String nome, String marca, String descricao,
    String categoria, String grupoPrecificacao, BigDecimal precoMin, BigDecimal precoMax,
    BigDecimal custoMin, BigDecimal custoMax, Integer estoqueMin, Integer estoqueMax,
    Boolean ativo, Boolean visivel) {}
