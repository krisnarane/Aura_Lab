package br.com.auralab.produtos.repository;

import br.com.auralab.produtos.api.dto.FiltroProduto;
import br.com.auralab.produtos.domain.Produto;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Predicados independentes da consulta RF0015. */
public final class ProdutoSpecifications {
  private ProdutoSpecifications() {}

  public static Specification<Produto> filtrar(FiltroProduto f) {
    return (root, query, cb) -> {
      var p = new ArrayList<jakarta.persistence.criteria.Predicate>();
      if (tem(f.codigo())) p.add(cb.equal(root.get("codigo"), f.codigo().trim()));
      if (tem(f.nome())) p.add(cb.like(cb.lower(root.get("nome")), trecho(f.nome()), '!'));
      if (tem(f.marca())) p.add(cb.like(cb.lower(root.get("marca")), trecho(f.marca()), '!'));
      if (tem(f.descricao())) p.add(cb.like(cb.lower(root.get("descricao")), trecho(f.descricao()), '!'));
      if (tem(f.categoria())) {
        var sub = query.subquery(Long.class);
        var produto = sub.from(Produto.class);
        var categoria = produto.join("categorias");
        sub.select(produto.get("id")).where(cb.equal(produto.get("id"), root.get("id")),
            cb.equal(categoria.get("codigo"), f.categoria().trim()));
        p.add(cb.exists(sub));
      }
      if (tem(f.grupoPrecificacao())) p.add(cb.equal(root.get("grupoPrecificacao").get("codigo"), f.grupoPrecificacao().trim()));
      if (f.precoMin() != null) p.add(cb.greaterThanOrEqualTo(root.get("preco"), f.precoMin()));
      if (f.precoMax() != null) p.add(cb.lessThanOrEqualTo(root.get("preco"), f.precoMax()));
      if (f.custoMin() != null) p.add(cb.greaterThanOrEqualTo(root.get("valorCusto"), f.custoMin()));
      if (f.custoMax() != null) p.add(cb.lessThanOrEqualTo(root.get("valorCusto"), f.custoMax()));
      if (f.estoqueMin() != null) p.add(cb.greaterThanOrEqualTo(root.get("estoque"), f.estoqueMin()));
      if (f.estoqueMax() != null) p.add(cb.lessThanOrEqualTo(root.get("estoque"), f.estoqueMax()));
      if (f.ativo() != null) p.add(cb.equal(root.get("ativo"), f.ativo()));
      if (f.visivel() != null) p.add(cb.equal(root.get("visivel"), f.visivel()));
      return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
    };
  }

  private static boolean tem(String s) { return s != null && !s.isBlank(); }
  private static String trecho(String s) {
    return "%" + s.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%")
        .replace("_", "!_") + "%";
  }
}
