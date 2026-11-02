package br.com.auralab.ia.api;

import br.com.auralab.ia.service.IaService;
import br.com.auralab.ia.service.IaService.Resposta;
import br.com.auralab.ia.repository.IaRepository.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Contrato HTTP do chatbot RNF0044; nenhuma rota chama repository diretamente. */
@RestController
@RequestMapping("/api/v1/clientes/{cliente}")
public class IaController {
  public record PreferenciasInput(List<String> categorias) {}
  public record PerguntaInput(String texto) {}
  public record AvaliacaoInput(Boolean util, String motivo) {}
  private final IaService service;

  public IaController(IaService service) { this.service = service; }

  @GetMapping("/preferencias")
  public List<String> preferencias(@PathVariable Long cliente) { return service.preferencias(cliente); }

  @PutMapping("/preferencias")
  public List<String> salvarPreferencias(@PathVariable Long cliente, @RequestBody PreferenciasInput input) {
    return service.salvarPreferencias(cliente, input.categorias());
  }

  @GetMapping("/chat/conversas")
  public List<Conversa> conversas(@PathVariable Long cliente) { return service.conversas(cliente); }

  @GetMapping("/chat/conversas/{conversa}")
  public List<Mensagem> mensagens(@PathVariable Long cliente, @PathVariable Long conversa) {
    return service.mensagens(cliente, conversa);
  }

  @PostMapping("/chat/conversas/mensagens")
  public Resposta primeira(@PathVariable Long cliente, @RequestBody PerguntaInput input) {
    return service.enviar(cliente, null, input.texto());
  }

  @PostMapping("/chat/conversas/{conversa}/mensagens")
  public Resposta enviar(@PathVariable Long cliente, @PathVariable Long conversa, @RequestBody PerguntaInput input) {
    return service.enviar(cliente, conversa, input.texto());
  }

  @PutMapping("/chat/respostas/{resposta}/avaliacao")
  public Mensagem avaliar(@PathVariable Long cliente, @PathVariable Long resposta,
      @RequestBody AvaliacaoInput input) {
    return service.avaliar(cliente, resposta, input.util(), input.motivo());
  }
}
