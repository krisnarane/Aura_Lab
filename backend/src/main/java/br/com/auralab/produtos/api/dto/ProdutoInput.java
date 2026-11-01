package br.com.auralab.produtos.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/** Entrada completa de cadastro e edição (RF0011/RF0014); código PRD- é gerado no banco (RNF0021). */
public record ProdutoInput(
    @NotBlank @Size(max = 150) String nome,
    @NotBlank @Size(max = 80) String marca,
    @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal preco,
    @NotNull @Min(0) Integer estoque,
    @NotNull Boolean visivel,
    @NotBlank String descricao,
    @NotBlank String imagemUrl,
    @NotEmpty List<@NotBlank String> categorias,
    @NotBlank String grupoPrecificacao) {}
