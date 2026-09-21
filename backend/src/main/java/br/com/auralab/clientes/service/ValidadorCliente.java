package br.com.auralab.clientes.service;

import java.time.LocalDate;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Normalizações e validações reutilizadas do cadastro: CPF com dígitos verificadores e
 * senha forte (RN0026, RNF0031, RNF0032), além de DDD, telefone, CEP e nascimento (RN0023
 * e regras complementares do projeto). Cada método retorna o valor normalizado ou lança
 * {@link RegraNegocioException} com código de erro.
 */
@Component
public class ValidadorCliente {

  // RNF0031: mínimo de 8 caracteres com maiúscula, minúscula e caractere especial.
  private static final Pattern SENHA =
      Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$");

  public String cpf(String raw) {
    String cpf = digitos(raw);
    if (cpf.length() != 11
        || cpf.chars().distinct().count() == 1
        || digito(cpf, 9) != cpf.charAt(9) - 48
        || digito(cpf, 10) != cpf.charAt(10) - 48)
      throw new RegraNegocioException("CPF_INVALIDO", "Informe um CPF válido.");
    return cpf;
  }

  private int digito(String cpf, int limite) {
    int soma = 0, peso = limite + 1;
    for (int i = 0; i < limite; i++) soma += (cpf.charAt(i) - 48) * peso--;
    int r = 11 - (soma % 11);
    return r >= 10 ? 0 : r;
  }

  public String ddd(String v) {
    String d = digitos(v);
    if (d.length() != 2)
      throw new RegraNegocioException("DDD_INVALIDO", "DDD deve ter dois dígitos.");
    return d;
  }

  public String telefone(String v) {
    String d = digitos(v);
    if (d.length() < 8 || d.length() > 9)
      throw new RegraNegocioException(
          "TELEFONE_INVALIDO", "Telefone deve ter oito ou nove dígitos.");
    return d;
  }

  public String cep(String v) {
    String d = digitos(v);
    if (d.length() != 8)
      throw new RegraNegocioException("CEP_INVALIDO", "CEP deve ter oito dígitos.");
    return d;
  }

  /** RNF0031/RNF0032: força mínima e confirmação idêntica; o limite de 72 bytes é do BCrypt (RNF0033). */
  public void senha(String senha, String confirmacao) {
    if (senha != null && senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new RegraNegocioException(
          "SENHA_LONGA", "A senha deve ter no máximo 72 bytes em UTF-8.");
    if (!SENHA.matcher(senha == null ? "" : senha).matches())
      throw new RegraNegocioException(
          "SENHA_FRACA",
          "A senha deve ter 8 caracteres, letra maiúscula, minúscula e caractere especial.");
    if (!senha.equals(confirmacao))
      throw new RegraNegocioException("CONFIRMACAO_SENHA", "A confirmação da senha não confere.");
  }

  public void nascimento(LocalDate d) {
    if (d == null || d.isAfter(LocalDate.now()))
      throw new RegraNegocioException(
          "NASCIMENTO_INVALIDO", "A data de nascimento não pode ser futura.");
  }

  public String digitos(String v) {
    if (v != null && !v.matches("[0-9 ()+.\\-]*"))
      throw new RegraNegocioException(
          "FORMATO_INVALIDO", "Use somente números e pontuação de formatação.");
    return v == null ? "" : v.replaceAll("\\D", "");
  }
}
