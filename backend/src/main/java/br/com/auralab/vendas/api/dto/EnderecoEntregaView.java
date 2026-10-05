package br.com.auralab.vendas.api.dto;

/** Saída do endereço de entrega do pedido (snapshot); enderecoId aponta o perfil quando houver. */
public record EnderecoEntregaView(
    Long enderecoId,
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
    String complemento,
    String observacoes) {}
