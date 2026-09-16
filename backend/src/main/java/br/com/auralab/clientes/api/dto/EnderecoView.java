package br.com.auralab.clientes.api.dto;

public record EnderecoView(
    Long id,
    String apelido,
    String tipoResidencia,
    String tipoLogradouro,
    String logradouro,
    String numero,
    String bairro,
    String cep,
    String cidade,
    String estado,
    String pais,
    String observacoes,
    String complemento,
    boolean cobranca,
    boolean entrega,
    boolean preferencialEntrega,
    boolean ativo) {}
