package br.com.auralab.produtos.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Entrada do cadastro mínimo de produto (RF0011 parcial): nome, marca, preço, estoque inicial e visibilidade; o código PRD- é gerado no banco (RNF0021). */
public record ProdutoInput(
    @NotBlank @Size(max = 150) String nome,
    @NotBlank @Size(max = 80) String marca,
    @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal preco,
    @NotNull @Min(0) Integer estoque,
    @NotNull Boolean visivel) {}
