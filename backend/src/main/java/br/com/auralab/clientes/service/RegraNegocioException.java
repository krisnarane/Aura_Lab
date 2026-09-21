package br.com.auralab.clientes.service;

import java.util.*;

/**
 * Exceção de regra de negócio com código estável. O status HTTP é derivado do código —
 * 400 para entrada inválida, 404 para ausência/associação incorreta, 409 para conflito —
 * e {@code campos} aponta o campo relacionado quando aplicável (contrato de erros do
 * módulo).
 */
public class RegraNegocioException extends RuntimeException {
  private final String codigo;
  private static final Set<String> AUSENTES =
      Set.of(
          "CLIENTE_NAO_ENCONTRADO",
          "ENDERECO_NAO_ENCONTRADO",
          "CARTAO_NAO_ENCONTRADO",
          "TRANSACAO_NAO_ENCONTRADA",
          "PRODUTO_NAO_ENCONTRADO");
  private static final Set<String> CONFLITOS =
      Set.of(
          "CLIENTE_DUPLICADO",
          "CLIENTE_INATIVO",
          "ENDERECO_INATIVO",
          "CARTAO_INATIVO",
          "ULTIMO_ENDERECO_COBRANCA",
          "ULTIMO_ENDERECO_ENTREGA",
          "PRODUTO_JA_ATIVO",
          "PRODUTO_JA_INATIVO");

  public RegraNegocioException(String codigo, String mensagem) {
    super(mensagem);
    this.codigo = codigo;
  }

  public String getCodigo() {
    return codigo;
  }

  public int getStatus() {
    return AUSENTES.contains(codigo) ? 404 : CONFLITOS.contains(codigo) ? 409 : 400;
  }

  public Map<String, String> getCampos() {
    String campo =
        switch (codigo) {
          case "DDD_INVALIDO" -> "telefoneDdd";
          case "TELEFONE_INVALIDO" -> "telefoneNumero";
          case "CARTAO_INVALIDO" -> "numero";
          case "CODIGO_SEGURANCA_INVALIDO" -> "codigoSeguranca";
          case "CPF_INVALIDO" -> "cpf";
          case "CEP_INVALIDO" -> "cep";
          case "NASCIMENTO_INVALIDO" -> "nascimento";
          case "BANDEIRA_INVALIDA" -> "bandeiraId";
          case "CATEGORIA_PRODUTO_INVALIDA" -> "categoria";
          case "CONFIRMACAO_SENHA" -> "confirmacaoSenha";
          case "SENHA_FRACA", "SENHA_LONGA" -> "senha";
          default -> null;
        };
    return campo == null ? Map.of() : Map.of(campo, getMessage());
  }
}
