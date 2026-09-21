package br.com.auralab.clientes.repository;

import br.com.auralab.clientes.domain.Cliente;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications da consulta de clientes (RF0024): filtros combinados com E. Nome e
 * e-mail usam busca parcial com `%`/`_` tratados como literais; os demais campos usam
 * igualdade normalizada.
 */
public final class ClienteSpecifications {
  private ClienteSpecifications() {}

  /** Compõe os predicados; filtros vazios são ignorados (combináveis livremente). */
  public static Specification<Cliente> filtrar(
      String codigo,
      String nome,
      String cpf,
      String email,
      String genero,
      LocalDate nascimento,
      String tipo,
      String ddd,
      String numero,
      Boolean ativo) {
    return Specification.allOf(
        igual("codigo", codigo),
        contem("nome", nome),
        igual("cpf", digitos(cpf)),
        contem("email", email),
        igual("genero", genero),
        nascimento == null ? null : (r, q, b) -> b.equal(r.get("nascimento"), nascimento),
        igual("telefoneTipo", tipo),
        igual("telefoneDdd", digitos(ddd)),
        igual("telefoneNumero", digitos(numero)),
        ativo == null ? null : (r, q, b) -> b.equal(r.get("ativo"), ativo));
  }

  // LIKE case-insensitive com escape de %/_ para busca parcial sem curingas indesejados.
  private static Specification<Cliente> contem(String campo, String valor) {
    return vazio(valor)
        ? null
        : (r, q, b) ->
            b.like(
                b.lower(r.get(campo)),
                "%"
                    + valor
                        .trim()
                        .toLowerCase(java.util.Locale.ROOT)
                        .replace("!", "!!")
                        .replace("%", "!%")
                        .replace("_", "!_")
                    + "%",
                '!');
  }

  private static Specification<Cliente> igual(String campo, String valor) {
    return vazio(valor)
        ? null
        : (r, q, b) -> b.equal(b.lower(r.get(campo)), valor.trim().toLowerCase());
  }

  private static boolean vazio(String v) {
    return v == null || v.isBlank();
  }

  private static String digitos(String v) {
    return v == null || v.isBlank()
        ? null
        : (v.replaceAll("\\D", "").isEmpty() ? "INVALIDO" : v.replaceAll("\\D", ""));
  }
}
