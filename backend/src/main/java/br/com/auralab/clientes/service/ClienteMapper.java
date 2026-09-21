package br.com.auralab.clientes.service;

import br.com.auralab.clientes.api.dto.*;
import br.com.auralab.clientes.domain.*;
import org.springframework.stereotype.Component;

/**
 * Converte entidades em DTOs de resposta — único ponto de saída do domínio para a API,
 * garantindo que senha/hash nunca apareçam nas respostas (RNF0033).
 */
@Component
public class ClienteMapper {
  public ClienteView view(Cliente c, int ranking) {
    return new ClienteView(
        c.getId(),
        c.getCodigo(),
        c.getNome(),
        c.getCpf(),
        c.getEmail(),
        c.getGenero(),
        c.getNascimento(),
        c.getTelefoneTipo(),
        c.getTelefoneDdd(),
        c.getTelefoneNumero(),
        c.isAtivo(),
        ranking,
        c.getEnderecos().stream().map(this::endereco).toList(),
        c.getCartoes().stream().map(this::cartao).toList());
  }

  public EnderecoView endereco(Endereco e) {
    return new EnderecoView(
        e.getId(),
        e.getApelido(),
        e.getTipoResidencia(),
        e.getTipoLogradouro(),
        e.getLogradouro(),
        e.getNumero(),
        e.getBairro(),
        e.getCep(),
        e.getCidade(),
        e.getEstado(),
        e.getPais(),
        e.getObservacoes(),
        e.getComplemento(),
        e.isCobranca(),
        e.isEntrega(),
        e.isPreferencialEntrega(),
        e.isAtivo());
  }

  public CartaoView cartao(Cartao c) {
    return new CartaoView(
        c.getId(),
        c.getBandeira().getId(),
        c.getBandeira().getNome(),
        c.getTitular(),
        c.getUltimosQuatro(),
        c.isPreferencial(),
        c.isAtivo());
  }
}
