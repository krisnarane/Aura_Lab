package br.com.auralab.produtos.api;

import br.com.auralab.produtos.api.dto.*;
import br.com.auralab.produtos.service.ProdutoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

  @PatchMapping("/produtos/{id}/inativacao")
  ProdutoView inativar(@PathVariable Long id, @Valid @RequestBody MotivoStatusProdutoInput in) {
    return produtos.inativar(id, in);
  }

  @PatchMapping("/produtos/{id}/ativacao")
  ProdutoView ativar(@PathVariable Long id, @Valid @RequestBody MotivoStatusProdutoInput in) {
    return produtos.ativar(id, in);
  }
}
