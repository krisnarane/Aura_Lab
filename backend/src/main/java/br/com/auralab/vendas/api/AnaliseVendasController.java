package br.com.auralab.vendas.api;

import br.com.auralab.clientes.api.dto.ErroView;
import br.com.auralab.vendas.service.AnaliseVendasService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Perfil demonstrativo é forjável e não representa autenticação. */
@RestController
@RequestMapping("/api/v1/admin/analise-vendas")
public class AnaliseVendasController {
  private final AnaliseVendasService service;
  public AnaliseVendasController(AnaliseVendasService service) { this.service = service; }

  @GetMapping
  ResponseEntity<?> consultar(@RequestHeader(value = "X-Demo-Perfil", required = false) String perfil,
      @RequestParam LocalDate inicio, @RequestParam LocalDate fim,
      @RequestParam List<String> categorias) {
    if (!"ADMIN_DEMO".equals(perfil))
      return ResponseEntity.status(403).body(new ErroView("ACESSO_NEGADO", "Selecione o perfil administrador para consultar a análise.", Map.of()));
    return ResponseEntity.ok(service.consultar(inicio, fim, categorias));
  }
}
