package br.com.auralab.produtos.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProdutoView(
    Long id,
    String codigo,
    String nome,
    String marca,
    BigDecimal preco,
    int estoque,
    boolean ativo,
    boolean visivel,
    String categoriaInativacao,
    String justificativaInativacao,
    OffsetDateTime inativadoEm,
    String categoriaAtivacao,
    String justificativaAtivacao,
    OffsetDateTime ativadoEm) {}
