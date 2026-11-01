package br.com.auralab.produtos.service;

import br.com.auralab.clientes.service.RegraNegocioException;
import br.com.auralab.produtos.api.dto.*;
import br.com.auralab.produtos.domain.Produto;
import br.com.auralab.produtos.domain.CategoriaProduto;
import br.com.auralab.produtos.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro, edição e consulta de produtos (RF0011/RF0014/RF0015), além do ciclo de situação
 * RF0012/RF0016. Escrita e auditoria compartilham a transação (RNF0012).
 */
@Service
public class ProdutoService {

  private final ProdutoRepository repo;
  private final CategoriaInativacaoProdutoRepository categoriasInativacao;
  private final CategoriaAtivacaoProdutoRepository categoriasAtivacao;
  private final CategoriaProdutoRepository categoriasProduto;
  private final GrupoPrecificacaoRepository grupos;
  private final AuditoriaProdutoService auditoria;
  private final br.com.auralab.vendas.repository.CicloVendaRepository ciclo;
  private final java.time.Clock clock;
  @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

  public ProdutoService(
      ProdutoRepository r,
      CategoriaInativacaoProdutoRepository ci,
      CategoriaAtivacaoProdutoRepository ca,
      CategoriaProdutoRepository cp,
      GrupoPrecificacaoRepository grupos,
      AuditoriaProdutoService a,
      br.com.auralab.vendas.repository.CicloVendaRepository ciclo,
      java.time.Clock clock) {
    repo = r;
    categoriasInativacao = ci;
    categoriasAtivacao = ca;
    categoriasProduto = cp;
    this.grupos = grupos;
    auditoria = a;
    this.ciclo = ciclo;
    this.clock = clock;
  }

  /** RF0015: código exato, nome parcial (`%`/`_` literais) e situação, combináveis. */
  @Transactional(readOnly = true)
  public List<ProdutoView> listar(FiltroProduto filtro) {
    var reservadas = ciclo.reservasPorProduto(java.time.OffsetDateTime.now(clock));
    return repo
        .findAll(ProdutoSpecifications.filtrar(filtro), org.springframework.data.domain.Sort.by("nome", "id"))
        .stream()
        .map(p -> view(p, reservadas.getOrDefault(p.getId(), 0)))
        .toList();
  }

  @Transactional(readOnly = true)
  public ProdutoView buscar(Long id) {
    return view(repo.findById(id).orElseThrow(this::naoEncontrado));
  }

  @Transactional
  public ProdutoView cadastrar(ProdutoInput in) {
    var categorias = validarCategorias(in.categorias());
    var grupo = validarGrupo(in.grupoPrecificacao());
    String imagem = validarImagem(in.imagemUrl());
    Produto p =
        new Produto(in.nome().trim(), in.marca().trim(), in.preco(), in.estoque(), in.visivel());
    p.editar(in.nome().trim(), in.marca().trim(), in.descricao().trim(), imagem,
        in.preco(), in.estoque(), in.visivel(), categorias, grupo);
    repo.saveAndFlush(p);
    em.refresh(p);
    auditoria.registrar(
        p.getId(), "CADASTRAR_PRODUTO", "PRODUTO", p.getId(), null, RegistroProduto.cadastro(p));
    return view(p);
  }

  @Transactional
  public ProdutoView editar(Long id, ProdutoInput in) {
    ciclo.bloquearFluxo();
    Produto p = repo.buscarParaAtualizacao(id).orElseThrow(this::naoEncontrado);
    if (!p.isAtivo() && in.visivel())
      throw new RegraNegocioException("VISIBILIDADE_PRODUTO_INATIVO", "Ative o produto antes de publicá-lo.");
    var categorias = validarCategorias(in.categorias());
    var grupo = validarGrupo(in.grupoPrecificacao());
    String imagem = validarImagem(in.imagemUrl());
    int reservadas = ciclo.reservasPorProduto(java.time.OffsetDateTime.now(clock)).getOrDefault(id, 0);
    if (in.estoque() < reservadas)
      throw new RegraNegocioException("ESTOQUE_INSUFICIENTE", "Estoque menor que as unidades reservadas.");
    Map<String, Object> antes = RegistroProduto.cadastro(p);
    p.editar(in.nome().trim(), in.marca().trim(), in.descricao().trim(), imagem,
        in.preco(), in.estoque(), in.visivel(), categorias, grupo);
    auditoria.registrar(id, "EDITAR_PRODUTO", "PRODUTO", id, antes, RegistroProduto.cadastro(p));
    return view(p);
  }

  private Set<CategoriaProduto> validarCategorias(List<String> codigos) {
    Set<String> unicos = new LinkedHashSet<>(codigos);
    if (unicos.size() != codigos.size())
      throw categoriasCadastroInvalidas();
    var categorias = categoriasProduto.findAllById(unicos);
    if (categorias.size() != unicos.size()) throw categoriasCadastroInvalidas();
    return new LinkedHashSet<>(categorias);
  }

  private br.com.auralab.produtos.domain.GrupoPrecificacao validarGrupo(String codigo) {
    return grupos.findById(codigo.trim()).orElseThrow(() ->
        new RegraNegocioException("GRUPO_PRECIFICACAO_INVALIDO", "Grupo de precificação não cadastrado."));
  }

  @Transactional(readOnly = true)
  public List<GrupoPrecificacaoView> gruposPrecificacao() {
    return grupos.findAll().stream().map(g -> new GrupoPrecificacaoView(g.getCodigo(),
        g.getDescricao(), g.getPercentualMargem())).toList();
  }

  private RegraNegocioException categoriasCadastroInvalidas() {
    return new RegraNegocioException("CATEGORIAS_PRODUTO_INVALIDAS", "Selecione categorias cadastradas, sem repetição.");
  }

  private String validarImagem(String url) {
    try {
      var uri = new java.net.URI(url.trim());
      if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
          || uri.getHost() == null || uri.getHost().isBlank() || uri.getUserInfo() != null)
        throw new IllegalArgumentException();
      return uri.toString();
    } catch (Exception ex) {
      throw new RegraNegocioException("IMAGEM_URL_INVALIDA", "Informe uma URL HTTP ou HTTPS válida.");
    }
  }

  @Transactional(readOnly = true)
  public List<CategoriaProdutoView> categoriasProduto() {
    return categoriasProduto.findAll().stream().map(c -> new CategoriaProdutoView(c.getCodigo(), c.getDescricao()))
        .sorted(java.util.Comparator.comparing(CategoriaProdutoView::descricao)).toList();
  }

  /**
   * RF0012/RN0015: a categoria precisa existir em categoria_inativacao_produto; inativar também
   * oculta (visivel = false).
   */
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

  /**
   * RF0016/RN0017: categoria precisa existir em categoria_ativacao_produto; ativar não republica
   * automaticamente.
   */
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

  private RegraNegocioException categoriaInvalida(String operacao) {
    return new RegraNegocioException(
        "CATEGORIA_PRODUTO_INVALIDA",
        "A categoria de " + operacao + " deve estar cadastrada no sistema.");
  }

  private RegraNegocioException naoEncontrado() {
    return new RegraNegocioException("PRODUTO_NAO_ENCONTRADO", "Produto não encontrado.");
  }

  private ProdutoView view(Produto p) {
    return view(
        p,
        ciclo.reservasPorProduto(java.time.OffsetDateTime.now(clock)).getOrDefault(p.getId(), 0));
  }

  private ProdutoView view(Produto p, int reservadas) {
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
        p.getAtivadoEm(),
        Math.max(0, p.getEstoque() - reservadas),
        p.getDescricao(), p.getImagemUrl(),
        p.getCategorias().stream().map(c -> new CategoriaProdutoView(c.getCodigo(), c.getDescricao()))
            .sorted(java.util.Comparator.comparing(CategoriaProdutoView::descricao)).toList(),
        new GrupoPrecificacaoView(p.getGrupoPrecificacao().getCodigo(),
            p.getGrupoPrecificacao().getDescricao(), p.getGrupoPrecificacao().getPercentualMargem()),
        p.getValorCusto());
  }
}
