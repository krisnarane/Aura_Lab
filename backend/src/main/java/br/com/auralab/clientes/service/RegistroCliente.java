package br.com.auralab.clientes.service;

import br.com.auralab.clientes.domain.*;
import java.util.*;

/**
 * Snapshots de auditoria com campos explicitamente permitidos (RNF0012). Nunca serialize
 * uma entidade ou requisição inteira: senha, hash, PAN e CVV ficam de fora por construção.
 */
public final class RegistroCliente {
  private RegistroCliente() {}

  public static Map<String, Object> pessoal(Cliente c) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("codigo", c.getCodigo());
    m.put("nome", c.getNome());
    m.put("cpf", c.getCpf());
    m.put("email", c.getEmail());
    m.put("genero", c.getGenero());
    m.put("nascimento", c.getNascimento().toString());
    m.put("telefoneTipo", c.getTelefoneTipo());
    m.put("telefoneDdd", c.getTelefoneDdd());
    m.put("telefoneNumero", c.getTelefoneNumero());
    m.put("ativo", c.isAtivo());
    return m;
  }

  public static Map<String, Object> enderecos(Cliente c) {
    return Map.of(
        "enderecos",
        c.getEnderecos().stream()
            .map(
                e -> {
                  Map<String, Object> m = new LinkedHashMap<>();
                  m.put("id", e.getId());
                  m.put("apelido", e.getApelido());
                  m.put("tipoResidencia", e.getTipoResidencia());
                  m.put("tipoLogradouro", e.getTipoLogradouro());
                  m.put("logradouro", e.getLogradouro());
                  m.put("numero", e.getNumero());
                  m.put("bairro", e.getBairro());
                  m.put("cep", e.getCep());
                  m.put("cidade", e.getCidade());
                  m.put("estado", e.getEstado());
                  m.put("pais", e.getPais());
                  m.put("complemento", e.getComplemento());
                  m.put("observacoes", e.getObservacoes());
                  m.put("cobranca", e.isCobranca());
                  m.put("entrega", e.isEntrega());
                  m.put("preferencialEntrega", e.isPreferencialEntrega());
                  m.put("ativo", e.isAtivo());
                  return m;
                })
            .toList());
  }

  public static Map<String, Object> cartoes(Cliente c) {
    return Map.of(
        "cartoes",
        c.getCartoes().stream()
            .map(
                x ->
                    Map.of(
                        "id",
                        x.getId(),
                        "titular",
                        x.getTitular(),
                        "bandeira",
                        x.getBandeira().getNome(),
                        "ultimosQuatro",
                        x.getUltimosQuatro(),
                        "preferencial",
                        x.isPreferencial(),
                        "ativo",
                        x.isAtivo()))
            .toList());
  }
}
