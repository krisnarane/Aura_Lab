package br.com.auralab.produtos.service;

import br.com.auralab.clientes.service.RegraNegocioException;
import br.com.auralab.produtos.api.dto.*;
import br.com.auralab.produtos.domain.Produto;
import br.com.auralab.produtos.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {

  private final ProdutoRepository repo;
  private final CategoriaInativacaoProdutoRepository categoriasInativacao;
  private final CategoriaAtivacaoProdutoRepository categoriasAtivacao;
  private final AuditoriaProdutoService auditoria;
  @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

  public ProdutoService(
      ProdutoRepository r,
      CategoriaInativacaoProdutoRepository ci,
      CategoriaAtivacaoProdutoRepository ca,
      AuditoriaProdutoService a) {
    repo = r;
    categoriasInativacao = ci;
    categoriasAtivacao = ca;
    auditoria = a;
  }

  @Transactional(readOnly = true)
  public List<ProdutoView> listar(String codigo, String nome, Boolean ativo) {
    String trecho =
        nome == null || nome.isBlank() ? null : "%" + escaparLike(nome.trim().toLowerCase(Locale.ROOT)) + "%";
    return repo.filtrar(
            codigo == null || codigo.isBlank() ? null : codigo.trim(), trecho, ativo)
        .stream()
        .map(this::view)
        .toList();
  }

  @Transactional(readOnly = true)
  public ProdutoView buscar(Long id) {
    return view(repo.findById(id).orElseThrow(this::naoEncontrado));
  }

  @Transactional
  public ProdutoView cadastrar(ProdutoInput in) {
    Produto p =
        new Produto(
            in.nome().trim(), in.marca().trim(), in.preco(), in.estoque(), in.visivel());
    repo.saveAndFlush(p);
    em.refresh(p);
    auditoria.registrar(
        p.getId(), "CADASTRAR_PRODUTO", "PRODUTO", p.getId(), null, RegistroProduto.cadastro(p));
    return view(p);
  }

  @Transactional
  public ProdutoView inativar(Long id, MotivoStatusProdutoInput in) {
    Produto p = repo.buscarParaAtualizacao(id).orElseThrow(this::naoEncontrado);
    if (!p.isAtivo())
      throw new RegraNegocioException("PRODUTO_JA_INATIVO", "Este produto já está inativo.");
    String categoria = in.categoria().trim();
    if (!categoriasInativacao.existsById(categoria)) throw categoriaInvalida("inativação");
    String justificativa = in.justificativa().trim();
    Map<String, Object> antes = RegistroProduto.status(p, null, null);
    p.inativar(categoria, justificativa);
    auditoria.registrar(
        id,
        "INATIVAR_PRODUTO",
        "PRODUTO",
        id,
        antes,
        RegistroProduto.status(p, categoria, justificativa));
    return view(p);
  }

  @Transactional
  public ProdutoView ativar(Long id, MotivoStatusProdutoInput in) {
    Produto p = repo.buscarParaAtualizacao(id).orElseThrow(this::naoEncontrado);
    if (p.isAtivo())
      throw new RegraNegocioException("PRODUTO_JA_ATIVO", "Este produto já está ativo.");
    String categoria = in.categoria().trim();
    if (!categoriasAtivacao.existsById(categoria)) throw categoriaInvalida("ativação");
    String justificativa = in.justificativa().trim();
    Map<String, Object> antes = RegistroProduto.status(p, null, null);
    p.ativar(categoria, justificativa);
    auditoria.registrar(
        id,
        "ATIVAR_PRODUTO",
        "PRODUTO",
        id,
        antes,
        RegistroProduto.status(p, categoria, justificativa));
    return view(p);
  }

  public Produto entidade(Long id) {
    return repo.findById(id).orElseThrow(this::naoEncontrado);
  }

  @Transactional(readOnly = true)
  public List<CategoriaProdutoView> categoriasAtivacao() {
    return categoriasAtivacao.findAll().stream()
        .map(c -> new CategoriaProdutoView(c.getCodigo(), c.getDescricao()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CategoriaProdutoView> categoriasInativacao() {
    return categoriasInativacao.findAll().stream()
        .map(c -> new CategoriaProdutoView(c.getCodigo(), c.getDescricao()))
        .toList();
  }

  private static String escaparLike(String valor) {
    return valor.replace("!", "!!").replace("%", "!%").replace("_", "!_");
  }

  private RegraNegocioException categoriaInvalida(String operacao) {
    return new RegraNegocioException(
        "CATEGORIA_PRODUTO_INVALIDA",
        "A categoria de " + operacao + " deve estar cadastrada no sistema.");
  }

  private RegraNegocioException naoEncontrado() {
    return new RegraNegocioException("PRODUTO_NAO_ENCONTRADO", "Produto não encontrado.");
  }

  private ProdutoView view(Produto p) {
    return new ProdutoView(
        p.getId(),
        p.getCodigo(),
        p.getNome(),
        p.getMarca(),
        p.getPreco(),
        p.getEstoque(),
        p.isAtivo(),
        p.isVisivel(),
        p.getCategoriaInativacao(),
        p.getJustificativaInativacao(),
        p.getInativadoEm(),
        p.getCategoriaAtivacao(),
        p.getJustificativaAtivacao(),
        p.getAtivadoEm());
  }
}
