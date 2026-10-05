package br.com.auralab.vendas.service;

import br.com.auralab.clientes.repository.ClienteRepository;
import br.com.auralab.clientes.service.AuditoriaService;
import br.com.auralab.clientes.service.RegraNegocioException;
import br.com.auralab.produtos.domain.Produto;
import br.com.auralab.produtos.repository.ProdutoRepository;
import br.com.auralab.vendas.api.dto.*;
import br.com.auralab.vendas.domain.*;
import br.com.auralab.vendas.repository.CarrinhoRepository;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service do carrinho de compra (RF0031/RF0032): adicionar, alterar quantidade, remover e
 * visualizar itens, validando a disponibilidade em estoque (RN0031) — não é permitido adicionar
 * produto indisponível nem quantidade superior ao estoque. Escrita e auditoria dividem a mesma
 * transação (RNF0012).
 */
@Service
public class CarrinhoService {

  private final CarrinhoRepository carrinhos;
  private final ProdutoRepository produtos;
  private final ClienteRepository clientes;
  private final AuditoriaService auditoria;

  public CarrinhoService(
      CarrinhoRepository carrinhos,
      ProdutoRepository produtos,
      ClienteRepository clientes,
      AuditoriaService auditoria) {
    this.carrinhos = carrinhos;
    this.produtos = produtos;
    this.clientes = clientes;
    this.auditoria = auditoria;
  }

  @Transactional(readOnly = true)
  public CarrinhoView obter(Long clienteId) {
    exigirCliente(clienteId);
    return view(carrinhos.findByClienteId(clienteId).orElse(null));
  }

  /** RF0031/RF0032 + RN0031: adiciona (ou soma à quantidade já existente) validando o estoque. */
  @Transactional
  public CarrinhoView adicionar(Long clienteId, ItemCarrinhoInput in) {
    exigirCliente(clienteId);
    Carrinho cart =
        carrinhos.buscarParaAtualizacao(clienteId).orElseGet(() -> criarCarrinho(clienteId));
    Map<String, Object> antes = RegistroVenda.carrinho(cart);
    Produto p = exigirProduto(in.produtoId());
    if (!p.isAtivo() || !p.isVisivel())
      throw new RegraNegocioException(
          "PRODUTO_INDISPONIVEL", "Este produto não está disponível para compra.");
    ItemCarrinho existente = cart.item(p.getId());
    int novaQtd = (existente == null ? 0 : existente.getQuantidade()) + in.quantidade();
    exigirEstoque(p, novaQtd);
    ItemCarrinho alvo;
    if (existente != null) {
      existente.definirQuantidade(novaQtd);
      cart.tocar();
      alvo = existente;
    } else {
      alvo = new ItemCarrinho(p.getId(), in.quantidade());
      cart.adicionar(alvo);
    }
    carrinhos.flush();
    auditoria.registrar(
        clienteId,
        "ADICIONAR_ITEM_CARRINHO",
        "CARRINHO",
        alvo.getId(),
        antes,
        RegistroVenda.carrinho(cart));
    return view(cart);
  }

  /** RF0032 + RN0031: define a quantidade de um item já no carrinho, revalidando o estoque. */
  @Transactional
  public CarrinhoView alterarQuantidade(Long clienteId, Long itemId, QuantidadeInput in) {
    exigirCliente(clienteId);
    Carrinho cart = exigirCarrinho(clienteId);
    Map<String, Object> antes = RegistroVenda.carrinho(cart);
    ItemCarrinho item = exigirItem(cart, itemId);
    Produto p = exigirProduto(item.getProdutoId());
    exigirEstoque(p, in.quantidade());
    item.definirQuantidade(in.quantidade());
    cart.tocar();
    carrinhos.flush();
    auditoria.registrar(
        clienteId,
        "ALTERAR_ITEM_CARRINHO",
        "CARRINHO",
        itemId,
        antes,
        RegistroVenda.carrinho(cart));
    return view(cart);
  }

  @Transactional
  public CarrinhoView remover(Long clienteId, Long itemId) {
    exigirCliente(clienteId);
    Carrinho cart = exigirCarrinho(clienteId);
    Map<String, Object> antes = RegistroVenda.carrinho(cart);
    ItemCarrinho item = exigirItem(cart, itemId);
    cart.remover(item);
    carrinhos.flush();
    auditoria.registrar(
        clienteId,
        "REMOVER_ITEM_CARRINHO",
        "CARRINHO",
        itemId,
        antes,
        RegistroVenda.carrinho(cart));
    return view(cart);
  }

  private Carrinho criarCarrinho(Long clienteId) {
    return carrinhos.saveAndFlush(new Carrinho(clienteId));
  }

  private Carrinho exigirCarrinho(Long clienteId) {
    return carrinhos
        .buscarParaAtualizacao(clienteId)
        .orElseThrow(
            () ->
                new RegraNegocioException(
                    "CARRINHO_NAO_ENCONTRADO", "Carrinho vazio ou inexistente."));
  }

  private Produto exigirProduto(Long id) {
    return produtos
        .findById(id)
        .orElseThrow(
            () -> new RegraNegocioException("PRODUTO_NAO_ENCONTRADO", "Produto não encontrado."));
  }

  private void exigirEstoque(Produto p, int quantidade) {
    if (quantidade > p.getEstoque())
      throw new RegraNegocioException(
          "ESTOQUE_INSUFICIENTE",
          "Quantidade indisponível: há " + p.getEstoque() + " unidade(s) em estoque.");
  }

  private ItemCarrinho exigirItem(Carrinho cart, Long itemId) {
    ItemCarrinho item = cart.itemPorId(itemId);
    if (item == null)
      throw new RegraNegocioException(
          "ITEM_CARRINHO_NAO_ENCONTRADO", "Item não encontrado no carrinho.");
    return item;
  }

  private void exigirCliente(Long clienteId) {
    if (!clientes.existsById(clienteId))
      throw new RegraNegocioException("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado.");
  }

  CarrinhoView view(Carrinho cart) {
    if (cart == null || cart.getItens().isEmpty())
      return new CarrinhoView(cart == null ? null : cart.getId(), List.of(), zero(), 0);
    List<Long> ids = cart.getItens().stream().map(ItemCarrinho::getProdutoId).toList();
    Map<Long, Produto> byId =
        produtos.findAllById(ids).stream()
            .collect(Collectors.toMap(Produto::getId, Function.identity()));
    List<ItemCarrinhoView> itens = new ArrayList<>();
    BigDecimal subtotal = zero();
    int unidades = 0;
    for (ItemCarrinho i : cart.getItens()) {
      Produto p = byId.get(i.getProdutoId());
      BigDecimal preco = p.getPreco();
      BigDecimal sub = preco.multiply(BigDecimal.valueOf(i.getQuantidade()));
      boolean disponivel = p.isAtivo() && p.isVisivel() && p.getEstoque() >= i.getQuantidade();
      itens.add(
          new ItemCarrinhoView(
              i.getId(),
              p.getId(),
              p.getCodigo(),
              p.getNome(),
              preco,
              i.getQuantidade(),
              sub,
              p.getEstoque(),
              disponivel));
      subtotal = subtotal.add(sub);
      unidades += i.getQuantidade();
    }
    return new CarrinhoView(cart.getId(), itens, subtotal, unidades);
  }

  private static BigDecimal zero() {
    return BigDecimal.ZERO.setScale(2);
  }
}
