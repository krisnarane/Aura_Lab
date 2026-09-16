package br.com.auralab.clientes.api.dto;

import jakarta.validation.constraints.*;
import java.time.*;

public record ClienteAlteracaoInput(
    @NotBlank @Size(max = 150) String nome,
    @NotBlank @Size(max = 20) String cpf,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 40) String genero,
    @NotNull @PastOrPresent LocalDate nascimento,
    @NotBlank @Size(max = 20) String telefoneTipo,
    @NotBlank @Size(max = 8) String telefoneDdd,
    @NotBlank @Size(max = 20) String telefoneNumero) {}
