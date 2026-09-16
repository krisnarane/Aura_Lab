package br.com.auralab.clientes.api.dto;

import jakarta.validation.constraints.*;

public record EnderecoInput(
    @NotBlank @Size(max = 60) String apelido,
    @NotBlank @Size(max = 40) String tipoResidencia,
    @NotBlank @Size(max = 40) String tipoLogradouro,
    @NotBlank @Size(max = 160) String logradouro,
    @NotBlank @Size(max = 20) String numero,
    @NotBlank @Size(max = 100) String bairro,
    @NotBlank @Size(max = 12) String cep,
    @NotBlank @Size(max = 100) String cidade,
    @NotBlank @Size(max = 60) String estado,
    @NotBlank @Size(max = 60) String pais,
    @Size(max = 500) String observacoes,
    @Size(max = 100) String complemento,
    boolean cobranca,
    boolean entrega,
    boolean preferencialEntrega) {}
