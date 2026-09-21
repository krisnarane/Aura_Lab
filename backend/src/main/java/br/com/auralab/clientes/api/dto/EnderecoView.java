package br.com.auralab.clientes.api.dto;

/** Saída de endereço com finalidades, preferência de entrega e situação (RF0026). */
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
