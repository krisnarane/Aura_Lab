package br.com.auralab.clientes.api.dto;

import java.time.*;
import java.util.List;

/** Saída do cliente com ranking calculado (RN0027); nunca carrega senha ou hash (RNF0033). */
public record ClienteView(
    Long id,
    String codigo,
    String nome,
    String cpf,
    String email,
    String genero,
    LocalDate nascimento,
    String telefoneTipo,
    String telefoneDdd,
    String telefoneNumero,
    boolean ativo,
    int ranking,
    List<EnderecoView> enderecos,
    List<CartaoView> cartoes) {}
