package br.com.auralab.produtos.api;

import br.com.auralab.produtos.api.dto.*;
import br.com.auralab.produtos.service.ProdutoService;
import br.com.auralab.produtos.service.EntradaEstoqueService;
import br.com.auralab.produtos.service.InativacaoAutomaticaProdutos;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST de produtos (livros no DRS). Apenas delega ao ProdutoService.
 *
 * <p>Cobre RF0011, RF0014, RF0015 e o ciclo RF0012/RN0015
 * (inativar) e RF0016/RN0017 (ativar), além das consultas das tabelas de domínio de
 * motivo. Erros seguem o contrato {codigo, mensagem, campos} do ApiExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1")
public class ProdutoController {
  private final ProdutoService produtos;
  private final EntradaEstoqueService entradas;
  private final InativacaoAutomaticaProdutos inativacaoAutomatica;

  public ProdutoController(ProdutoService p, EntradaEstoqueService entradas,
      InativacaoAutomaticaProdutos inativacaoAutomatica) {
    produtos = p;
    this.entradas = entradas;
    this.inativacaoAutomatica = inativacaoAutomatica;
  }

  @GetMapping("/produtos")
  List<ProdutoView> listar(@ModelAttribute FiltroProduto filtro) {
    return produtos.listar(filtro);
  }

  @GetMapping("/produtos/{id}")
  ProdutoView buscar(@PathVariable Long id) {
    return produtos.buscar(id);
  }

  @GetMapping("/categorias-ativacao-produto")
  List<CategoriaProdutoView> categoriasAtivacao() {
    return produtos.categoriasAtivacao();
  }

  @GetMapping("/categorias-produto")
  List<CategoriaProdutoView> categoriasProduto() { return produtos.categoriasProduto(); }

  @GetMapping("/grupos-precificacao")
  List<GrupoPrecificacaoView> gruposPrecificacao() { return produtos.gruposPrecificacao(); }

  @PostMapping("/produtos/inativacao-automatica")
  List<Long> inativacaoAutomatica() { return inativacaoAutomatica.executar(); }

  @GetMapping("/produtos/{id}/entradas-estoque")
  List<EntradaEstoqueView> entradas(@PathVariable Long id) { return entradas.listar(id); }

  @PostMapping("/produtos/{id}/entradas-estoque")
  ResponseEntity<EntradaEstoqueView> registrarEntrada(@PathVariable Long id,
      @RequestBody EntradaEstoqueInput in) {
    var v = entradas.registrar(id, in);
    return ResponseEntity.created(URI.create("/api/v1/produtos/" + id + "/entradas-estoque/" + v.id())).body(v);
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

  @PatchMapping("/produtos/{id}")
  ProdutoView editar(@PathVariable Long id, @Valid @RequestBody ProdutoInput in) {
    return produtos.editar(id, in);
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
