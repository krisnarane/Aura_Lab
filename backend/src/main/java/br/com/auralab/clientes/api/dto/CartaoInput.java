package br.com.auralab.clientes.api.dto;

import jakarta.validation.constraints.*;

public record CartaoInput(
    @NotBlank @Size(max = 30) String numero,
    @NotBlank @Size(max = 150) String titular,
    @NotNull Long bandeiraId,
    @NotBlank @Size(max = 4) String codigoSeguranca,
    boolean preferencial) {}
