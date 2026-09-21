package br.com.auralab.produtos.api;

import br.com.auralab.produtos.api.dto.*;
import br.com.auralab.produtos.service.ProdutoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST de produtos (livros no DRS). Apenas delega ao ProdutoService.
 *
 * <p>Cobre RF0011 (cadastro parcial), RF0015 (consulta parcial) e o ciclo RF0012/RN0015
 * (inativar) e RF0016/RN0017 (ativar), além das consultas das tabelas de domínio de
 * motivo. Erros seguem o contrato {codigo, mensagem, campos} do ApiExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1")
public class ProdutoController {
  private final ProdutoService produtos;

  public ProdutoController(ProdutoService p) {
    produtos = p;
  }

  @GetMapping("/produtos")
  List<ProdutoView> listar(
      @RequestParam(required = false) String codigo,
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) Boolean ativo) {
    return produtos.listar(codigo, nome, ativo);
  }

  @GetMapping("/produtos/{id}")
  ProdutoView buscar(@PathVariable Long id) {
    return produtos.buscar(id);
  }

  @GetMapping("/categorias-ativacao-produto")
  List<CategoriaProdutoView> categoriasAtivacao() {
    return produtos.categoriasAtivacao();
  }

  @GetMapping("/categorias-inativacao-produto")
  List<CategoriaProdutoView> categoriasInativacao() {
    return produtos.categoriasInativacao();
  }

  @PostMapping("/produtos")
  ResponseEntity<ProdutoView> cadastrar(@Valid @RequestBody ProdutoInput in) {
    var v = produtos.cadastrar(in);
    return ResponseEntity.created(URI.create("/api/v1/produtos/" + v.id())).body(v);
  }

  /** RF0012/RN0015: inativação lógica com categoria e justificativa obrigatórias. */
  @PatchMapping("/produtos/{id}/inativacao")
  ProdutoView inativar(@PathVariable Long id, @Valid @RequestBody MotivoStatusProdutoInput in) {
    return produtos.inativar(id, in);
  }

  /** RF0016/RN0017: ativação lógica com categoria e justificativa obrigatórias. */
  @PatchMapping("/produtos/{id}/ativacao")
  ProdutoView ativar(@PathVariable Long id, @Valid @RequestBody MotivoStatusProdutoInput in) {
    return produtos.ativar(id, in);
  }
}
