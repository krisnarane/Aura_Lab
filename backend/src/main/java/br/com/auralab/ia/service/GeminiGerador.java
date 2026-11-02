package br.com.auralab.ia.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HttpOptions;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Usa ADC do processo servidor. Nenhuma credencial e enviada ao navegador. */
@Component
public class GeminiGerador implements GeradorIa {
  private static final Pattern ENDPOINT =
      Pattern.compile("projects/[^/]+/locations/([^/]+)/endpoints/[^/]+");
  private final String projeto;
  private final int timeout;
  private final int maxTokens;

  public GeminiGerador(
      @Value("${aura.ia.projeto:}") String projeto,
      @Value("${aura.ia.timeout-ms:20000}") int timeout,
      @Value("${aura.ia.max-output-tokens:512}") int maxTokens) {
    this.projeto = projeto;
    this.timeout = timeout;
    this.maxTokens = maxTokens;
  }

  @Override
  public String gerar(String endpoint, String prompt) {
    if (projeto.isBlank()) throw new IaIndisponivelException();
    var destino = ENDPOINT.matcher(endpoint == null ? "" : endpoint);
    if (!destino.matches()) throw new IaIndisponivelException();
    try (Client cliente = Client.builder()
        .project(projeto).location(destino.group(1)).enterprise(true)
        .httpOptions(HttpOptions.builder().apiVersion("v1").timeout(timeout).build())
        .build()) {
      String texto = cliente.models.generateContent(endpoint, prompt,
          GenerateContentConfig.builder().maxOutputTokens(maxTokens).candidateCount(1).build()).text();
      if (texto == null || texto.isBlank()) throw new IaIndisponivelException();
      return texto;
    } catch (Exception ex) {
      throw new IaIndisponivelException();
    }
  }
}
