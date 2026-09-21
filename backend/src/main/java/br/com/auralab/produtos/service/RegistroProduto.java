package br.com.auralab.produtos.service;

import br.com.auralab.produtos.domain.Produto;
import java.util.*;

/**
 * Snapshots de auditoria de produto com campos explicitamente permitidos (RNF0012):
 * situação, visibilidade e motivo. Nunca serialize a entidade inteira.
 */
public final class RegistroProduto {
  private RegistroProduto() {}

  public static Map<String, Object> cadastro(Produto p) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("codigo", p.getCodigo());
    m.put("nome", p.getNome());
    m.put("marca", p.getMarca());
    m.put("preco", p.getPreco());
    m.put("estoque", p.getEstoque());
    m.put("ativo", p.isAtivo());
    m.put("visivel", p.isVisivel());
    return m;
  }

  public static Map<String, Object> status(
      Produto p, String categoria, String justificativa) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("ativo", p.isAtivo());
    m.put("visivel", p.isVisivel());
    if (categoria != null) {
      m.put("categoria", categoria);
      m.put("justificativa", justificativa);
    }
    return m;
  }
}
