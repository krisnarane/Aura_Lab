package br.com.auralab.vendas.service;

import br.com.auralab.clientes.api.dto.CartaoView;
import br.com.auralab.clientes.api.dto.EnderecoInput;
import br.com.auralab.clientes.api.dto.EnderecoView;
import br.com.auralab.clientes.domain.*;
import br.com.auralab.clientes.repository.ClienteRepository;
import br.com.auralab.clientes.service.*;
import br.com.auralab.produtos.domain.Produto;
import br.com.auralab.produtos.repository.ProdutoRepository;
import br.com.auralab.vendas.api.dto.*;
import br.com.auralab.vendas.domain.*;
import br.com.auralab.vendas.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de realização e finalização da compra (RF0033/RF0038). Reúne endereço de entrega
 * (RF0035/RN0023), cálculo de frete (RF0034 via {@link PoliticaFrete}), composição de pagamento com
 * cartões e cupons (RF0036/RF0037) e as regras de pagamento RN0033 (um cupom promocional), RN0034
 * (mínimo por cartão), RN0035 (um único cartão abaixo do mínimo, apenas quando há cupons) e RN0036
 * (cupom de troco sem cupons desnecessários). O pedido é criado com status EM_PROCESSAMENTO;
 * validação/aprovação do pagamento e transições seguintes estão fora do escopo desta fase. Escrita
 * e auditoria compartilham a mesma transação (RNF0012).
 */
@Service
public class VendaService {

  private static final BigDecimal MIN_CARTAO = new BigDecimal("10.00");

  private final CarrinhoRepository carrinhos;
  private final ProdutoRepository produtos;
  private final PedidoRepository pedidos;
  private final CupomRepository cupons;
  private final ClienteRepository clientes;
  private final EnderecoService enderecos;
  private final CartaoService cartoes;
  private final ValidadorCliente validador;
  private final PoliticaFrete politicaFrete;
  private final AuditoriaService auditoria;

  @PersistenceContext private EntityManager em;

  public VendaService(
      CarrinhoRepository carrinhos,
      ProdutoRepository produtos,
      PedidoRepository pedidos,
      CupomRepository cupons,
      ClienteRepository clientes,
      EnderecoService enderecos,
      CartaoService cartoes,
      ValidadorCliente validador,
      PoliticaFrete politicaFrete,
      AuditoriaService auditoria) {
    this.carrinhos = carrinhos;
    this.produtos = produtos;
    this.pedidos = pedidos;
    this.cupons = cupons;
    this.clientes = clientes;
    this.enderecos = enderecos;
    this.cartoes = cartoes;
    this.validador = validador;
    this.politicaFrete = politicaFrete;
    this.auditoria = auditoria;
  }

  /** RF0034: pré-cálculo de subtotal, desconto do cupom promocional, frete e total a pagar. */
  @Transactional(readOnly = true)
  public ResumoView resumo(Long clienteId, ResumoInput in) {
    Cliente cliente = exigirCliente(clienteId);
    List<ItemCalc> itens =
        itensDoCarrinho(carrinhos.findByClienteId(clienteId).orElse(null), false);
    BigDecimal subtotal = somaSubtotais(itens);
    int unidades = somaUnidades(itens);
    String estado = null;
    if (in.enderecoId() != null) estado = enderecoDoPerfil(cliente, in.enderecoId()).getEstado();
    else if (in.estado() != null && !in.estado().isBlank()) estado = in.estado().trim();
    BigDecimal desconto = descontoPromocional(in.cupomPromocional(), subtotal).valor();
    BigDecimal frete = politicaFrete.calcular(estado, unidades, subtotal);
    BigDecimal total = subtotal.subtract(desconto).add(frete).max(zero());
    return new ResumoView(subtotal, unidades, desconto, frete, total);
  }

  /** RF0033/RF0038 + RN0033–RN0036: cria o pedido EM_PROCESSAMENTO a partir do carrinho. */
  @Transactional
  public PedidoView finalizar(Long clienteId, CheckoutInput in) {
    Cliente cliente =
        clientes
            .buscarParaAtualizacao(clienteId)
            .orElseThrow(
                () ->
                    new RegraNegocioException("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado."));
    Carrinho cart = carrinhos.buscarParaAtualizacao(clienteId).orElse(null);
    if (cart == null || cart.getItens().isEmpty())
      throw new RegraNegocioException("CARRINHO_VAZIO", "O carrinho está vazio.");

    List<ItemCalc> itens = itensDoCarrinho(cart, true);
    BigDecimal subtotal = somaSubtotais(itens);
    int unidades = somaUnidades(itens);

    ResolucaoEndereco endereco = resolverEndereco(cliente, clienteId, in);
    BigDecimal frete = politicaFrete.calcular(endereco.estado(), unidades, subtotal);

    Desconto promo = descontoPromocional(in.cupomPromocional(), subtotal);
    BigDecimal total = subtotal.subtract(promo.valor()).add(frete).max(zero());

    List<Cupom> cuponsTroca = resolverCuponsTroca(clienteId, in.cuponsTroca());
    BigDecimal somaCupons =
        cuponsTroca.stream().map(Cupom::getValor).reduce(zero(), BigDecimal::add);
    List<ParcelaResolvida> parcelas = resolverCartoes(cliente, clienteId, in.cartoes());
    BigDecimal somaCartoes =
        parcelas.stream().map(ParcelaResolvida::valor).reduce(zero(), BigDecimal::add);

    BigDecimal troco =
        validarComposicao(total, cuponsTroca, somaCupons, parcelas, somaCartoes, promo);

    Pedido pedido =
        new Pedido(
            clienteId,
            subtotal,
            promo.valor(),
            frete,
            total,
            promo.cupom() == null ? null : promo.cupom().getId(),
            endereco.enderecoId(),
            endereco.snapshot());
    for (ItemCalc ic : itens)
      pedido.adicionarItem(
          new ItemPedido(
              ic.produto().getId(),
              ic.produto().getCodigo(),
              ic.produto().getNome(),
              ic.produto().getPreco(),
              ic.quantidade()));
    for (Cupom c : cuponsTroca)
      pedido.adicionarParte(
          PagamentoParte.cupomTroca(c.getValor(), "Cupom de troca " + c.getCodigo(), c.getId()));
    for (ParcelaResolvida pc : parcelas)
      pedido.adicionarParte(PagamentoParte.cartao(pc.valor(), pc.descricao(), pc.cartaoId()));
    pedidos.saveAndFlush(pedido);
    em.refresh(pedido);

    Cupom trocoCupom = null;
    if (troco.compareTo(zero()) > 0) {
      trocoCupom =
          cupons.saveAndFlush(
              Cupom.troco(clienteId, troco, "Troco do pedido " + pedido.getCodigo()));
      em.refresh(trocoCupom);
      pedido.registrarTrocoGerado(trocoCupom.getId());
    }
    cuponsTroca.forEach(Cupom::utilizar);
    cupons.flush();

    cart.esvaziar();
    carrinhos.flush();

    auditoria.registrar(
        clienteId,
        "FINALIZAR_COMPRA",
        "PEDIDO",
        pedido.getId(),
        null,
        RegistroVenda.pedido(pedido));
    return pedidoView(pedido, trocoCupom);
  }

  @Transactional(readOnly = true)
  public PedidoView buscarPedido(Long clienteId, Long pedidoId) {
    Pedido pedido =
        pedidos
            .findByIdAndClienteId(pedidoId, clienteId)
            .orElseThrow(
                () -> new RegraNegocioException("PEDIDO_NAO_ENCONTRADO", "Pedido não encontrado."));
    return pedidoView(pedido, cupomOpcional(pedido.getCupomTrocaGeradoId()));
  }

  /**
   * RNF0011: os cupons de troco são buscados de uma só vez, em vez de uma consulta por pedido, para
   * que o tempo não cresça com o histórico do cliente.
   */
  @Transactional(readOnly = true)
  public List<PedidoView> listarPedidos(Long clienteId) {
    exigirCliente(clienteId);
    List<Pedido> lista = pedidos.findByClienteIdOrderByCriadoEmDesc(clienteId);
    List<Long> trocos =
        lista.stream().map(Pedido::getCupomTrocaGeradoId).filter(Objects::nonNull).toList();
    Map<Long, Cupom> porId =
        trocos.isEmpty()
            ? Map.of()
            : cupons.findAllById(trocos).stream()
                .collect(Collectors.toMap(Cupom::getId, Function.identity()));
    return lista.stream()
        .map(
            p ->
                pedidoView(
                    p,
                    p.getCupomTrocaGeradoId() == null
                        ? null
                        : porId.get(p.getCupomTrocaGeradoId())))
        .toList();
  }

  /** Cupons de troca ATIVOS do cliente (créditos disponíveis e gerados como troco). */
  @Transactional(readOnly = true)
  public List<CupomView> cuponsDoCliente(Long clienteId) {
    exigirCliente(clienteId);
    return cupons.findByClienteIdAndTipoOrderByIdDesc(clienteId, Cupom.TROCA).stream()
        .map(this::cupomView)
        .toList();
  }

  // ---- regras de composição de pagamento ----------------------------------------------------

  /**
   * Valida a composição e devolve o troco (RN0033–RN0036). Se os cupons de troca cobrem o total,
   * nenhum cartão é aceito e cada cupom deve ser necessário (RN0036), gerando troco pela diferença.
   * Caso contrário, os cartões cobrem o restante exato e vale o mínimo de R$ 10,00 por cartão
   * (RN0034). A RN0035 abre uma única exceção: ao combinar cupons com cartões, "é permitido que
   * seja realizado <b>um</b> pagamento de um valor menor que R$ 10,00 no cartão" — ou seja, no
   * máximo uma parcela fica abaixo do mínimo, e só quando há cupons na composição.
   */
  private BigDecimal validarComposicao(
      BigDecimal total,
      List<Cupom> cuponsTroca,
      BigDecimal somaCupons,
      List<ParcelaResolvida> parcelas,
      BigDecimal somaCartoes,
      Desconto promo) {
    if (somaCupons.compareTo(total) >= 0) {
      if (somaCartoes.compareTo(zero()) != 0)
        throw new RegraNegocioException(
            "COMPOSICAO_INVALIDA",
            "Os cupons de troca já cobrem o total; remova os cartões da composição.");
      for (Cupom c : cuponsTroca)
        if (somaCupons.subtract(c.getValor()).compareTo(total) >= 0)
          throw new RegraNegocioException(
              "CUPONS_EXCEDENTES",
              "Não utilize cupons desnecessários: uma combinação menor já cobre a compra.");
      return somaCupons.subtract(total);
    }
    BigDecimal restante = total.subtract(somaCupons);
    if (somaCartoes.compareTo(restante) != 0)
      throw new RegraNegocioException(
          "COMPOSICAO_INVALIDA",
          "A soma dos cupons e dos cartões deve fechar exatamente o total da compra.");
    boolean combinaComCupons = !cuponsTroca.isEmpty() || promo.valor().compareTo(zero()) > 0;
    long abaixoDoMinimo =
        parcelas.stream().filter(pc -> pc.valor().compareTo(MIN_CARTAO) < 0).count();
    if (abaixoDoMinimo > 0 && !combinaComCupons)
      throw new RegraNegocioException(
          "CARTAO_MINIMO", "Cada cartão deve receber no mínimo R$ 10,00.");
    if (abaixoDoMinimo > 1)
      throw new RegraNegocioException(
          "CARTAO_MINIMO", "Ao combinar cupons, apenas um cartão pode receber menos de R$ 10,00.");
    return zero();
  }

  private Desconto descontoPromocional(String codigo, BigDecimal subtotal) {
    if (codigo == null || codigo.isBlank()) return new Desconto(null, zero());
    Cupom promo =
        cupons
            .findByCodigoAndTipo(codigo.trim(), Cupom.PROMOCIONAL)
            .orElseThrow(
                () ->
                    new RegraNegocioException(
                        "CUPOM_NAO_ENCONTRADO", "Cupom promocional não encontrado."));
    if (!promo.valido(LocalDate.now()))
      throw new RegraNegocioException("CUPOM_INVALIDO", "Cupom promocional inválido ou expirado.");
    BigDecimal desconto =
        promo.getPercentual() != null
            ? subtotal
                .multiply(promo.getPercentual())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
            : promo.getValor() == null ? zero() : promo.getValor();
    return new Desconto(promo, desconto.min(subtotal).setScale(2, RoundingMode.HALF_UP));
  }

  private List<Cupom> resolverCuponsTroca(Long clienteId, List<Long> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    List<Long> distintos = ids.stream().distinct().toList();
    List<Cupom> encontrados = cupons.findAllById(distintos);
    if (encontrados.size() != distintos.size())
      throw new RegraNegocioException("CUPOM_NAO_ENCONTRADO", "Cupom de troca não encontrado.");
    LocalDate hoje = LocalDate.now();
    for (Cupom c : encontrados) {
      if (!c.isTroca() || !clienteId.equals(c.getClienteId()))
        throw new RegraNegocioException(
            "CUPOM_NAO_ENCONTRADO", "Cupom de troca não encontrado para este cliente.");
      if (!c.valido(hoje))
        throw new RegraNegocioException(
            "CUPOM_INVALIDO", "Cupom de troca " + c.getCodigo() + " indisponível ou expirado.");
    }
    return encontrados;
  }

  private List<ParcelaResolvida> resolverCartoes(
      Cliente cliente, Long clienteId, List<ParcelaCartaoInput> parcelas) {
    if (parcelas == null) return List.of();
    List<ParcelaResolvida> resolvidas = new ArrayList<>();
    for (ParcelaCartaoInput p : parcelas) {
      BigDecimal valor = p.valor() == null ? zero() : p.valor().setScale(2, RoundingMode.HALF_UP);
      if (valor.compareTo(zero()) <= 0) continue; // parcela sem valor é ignorada
      boolean existente = p.cartaoId() != null;
      boolean novo = p.novoCartao() != null;
      if (existente == novo)
        throw new RegraNegocioException(
            "COMPOSICAO_INVALIDA", "Cada parcela deve usar um cartão salvo ou um novo cartão.");
      if (existente) {
        Cartao card =
            cliente.getCartoes().stream()
                .filter(x -> x.getId().equals(p.cartaoId()))
                .findFirst()
                .orElseThrow(
                    () ->
                        new RegraNegocioException(
                            "CARTAO_NAO_ENCONTRADO", "Cartão não encontrado para este cliente."));
        if (!card.isAtivo())
          throw new RegraNegocioException("CARTAO_INATIVO", "Cartão inativo não pode ser usado.");
        resolvidas.add(
            new ParcelaResolvida(
                valor,
                card.getBandeira().getNome() + " final " + card.getUltimosQuatro(),
                card.getId()));
      } else if (p.incorporarCartao()) {
        CartaoView v = cartoes.adicionar(clienteId, p.novoCartao());
        resolvidas.add(
            new ParcelaResolvida(valor, v.bandeira() + " final " + v.ultimosQuatro(), v.id()));
      } else {
        CartaoService.DadosCartaoValidado d = cartoes.validar(p.novoCartao());
        resolvidas.add(
            new ParcelaResolvida(
                valor, d.bandeira().getNome() + " final " + d.ultimosQuatro(), null));
      }
    }
    return resolvidas;
  }

  // ---- endereço de entrega ------------------------------------------------------------------

  private ResolucaoEndereco resolverEndereco(Cliente cliente, Long clienteId, CheckoutInput in) {
    boolean existente = in.enderecoId() != null;
    boolean novo = in.novoEndereco() != null;
    if (existente == novo)
      throw new RegraNegocioException(
          "ENDERECO_OBRIGATORIO", "Selecione um endereço de entrega do perfil ou informe um novo.");
    if (existente) {
      Endereco e = enderecoDoPerfil(cliente, in.enderecoId());
      if (!e.isAtivo())
        throw new RegraNegocioException("ENDERECO_INATIVO", "Endereço inativo não pode ser usado.");
      if (!e.isEntrega())
        throw new RegraNegocioException(
            "ENDERECO_ENTREGA_INVALIDO", "O endereço selecionado não é de entrega.");
      return new ResolucaoEndereco(e.getId(), snapshot(e), e.getEstado());
    }
    EnderecoInput ni = in.novoEndereco();
    String cep = validador.cep(ni.cep());
    String estado = ni.estado().trim().toUpperCase(Locale.ROOT);
    Long enderecoId = null;
    if (in.incorporarEndereco()) {
      EnderecoInput entregaInput =
          new EnderecoInput(
              ni.apelido(),
              ni.tipoResidencia(),
              ni.tipoLogradouro(),
              ni.logradouro(),
              ni.numero(),
              ni.bairro(),
              ni.cep(),
              ni.cidade(),
              ni.estado(),
              ni.pais(),
              ni.observacoes(),
              ni.complemento(),
              ni.cobranca(),
              true,
              ni.preferencialEntrega());
      EnderecoView v = enderecos.adicionar(clienteId, entregaInput);
      enderecoId = v.id();
    }
    EnderecoEntrega snap =
        new EnderecoEntrega(
            texto(ni.apelido()),
            ni.tipoResidencia().trim(),
            ni.tipoLogradouro().trim(),
            ni.logradouro().trim(),
            ni.numero().trim(),
            ni.bairro().trim(),
            cep,
            ni.cidade().trim(),
            estado,
            ni.pais().trim(),
            texto(ni.complemento()),
            texto(ni.observacoes()));
    return new ResolucaoEndereco(enderecoId, snap, estado);
  }

  private Endereco enderecoDoPerfil(Cliente cliente, Long enderecoId) {
    return cliente.getEnderecos().stream()
        .filter(e -> e.getId().equals(enderecoId))
        .findFirst()
        .orElseThrow(
            () ->
                new RegraNegocioException(
                    "ENDERECO_NAO_ENCONTRADO", "Endereço não encontrado para este cliente."));
  }

  private EnderecoEntrega snapshot(Endereco e) {
    return new EnderecoEntrega(
        e.getApelido(),
        e.getTipoResidencia(),
        e.getTipoLogradouro(),
        e.getLogradouro(),
        e.getNumero(),
        e.getBairro(),
        e.getCep(),
        e.getCidade(),
        e.getEstado(),
        e.getPais(),
        e.getComplemento(),
        e.getObservacoes());
  }

  // ---- carrinho -> cálculo ------------------------------------------------------------------

  private List<ItemCalc> itensDoCarrinho(Carrinho cart, boolean validarEstoque) {
    if (cart == null) return List.of();
    List<ItemCalc> calc = new ArrayList<>();
    for (ItemCarrinho i : cart.getItens()) {
      Produto p =
          produtos
              .findById(i.getProdutoId())
              .orElseThrow(
                  () ->
                      new RegraNegocioException(
                          "PRODUTO_NAO_ENCONTRADO", "Produto não encontrado."));
      if (validarEstoque) {
        if (!p.isAtivo() || !p.isVisivel())
          throw new RegraNegocioException(
              "PRODUTO_INDISPONIVEL", "O produto " + p.getNome() + " não está disponível.");
        if (i.getQuantidade() > p.getEstoque())
          throw new RegraNegocioException(
              "ESTOQUE_INSUFICIENTE",
              "Estoque insuficiente para " + p.getNome() + " (" + p.getEstoque() + " disponível).");
      }
      calc.add(
          new ItemCalc(
              p, i.getQuantidade(), p.getPreco().multiply(BigDecimal.valueOf(i.getQuantidade()))));
    }
    return calc;
  }

  private BigDecimal somaSubtotais(List<ItemCalc> itens) {
    return itens.stream().map(ItemCalc::subtotal).reduce(zero(), BigDecimal::add);
  }

  private int somaUnidades(List<ItemCalc> itens) {
    return itens.stream().mapToInt(ItemCalc::quantidade).sum();
  }

  // ---- views --------------------------------------------------------------------------------

  private PedidoView pedidoView(Pedido p, Cupom troco) {
    EnderecoEntrega e = p.getEntrega();
    EnderecoEntregaView entrega =
        new EnderecoEntregaView(
            p.getEntregaEnderecoId(),
            e.getApelido(),
            e.getTipoResidencia(),
            e.getTipoLogradouro(),
            e.getLogradouro(),
            e.getNumero(),
            e.getBairro(),
            e.getCep(),
            e.getCidade(),
            e.getEstado(),
            e.getPais(),
            e.getComplemento(),
            e.getObservacoes());
    List<ItemPedidoView> itens =
        p.getItens().stream()
            .map(
                i ->
                    new ItemPedidoView(
                        i.getProdutoId(),
                        i.getProdutoCodigo(),
                        i.getProdutoNome(),
                        i.getPrecoUnitario(),
                        i.getQuantidade(),
                        i.getSubtotal()))
            .toList();
    List<PagamentoParteView> partes =
        p.getPartes().stream()
            .map(x -> new PagamentoParteView(x.getTipo(), x.getDescricao(), x.getValor()))
            .toList();
    return new PedidoView(
        p.getId(),
        p.getCodigo(),
        p.getStatus(),
        p.getSubtotal(),
        p.getDesconto(),
        p.getFrete(),
        p.getTotal(),
        entrega,
        itens,
        partes,
        troco == null ? null : cupomView(troco),
        p.getCriadoEm());
  }

  private CupomView cupomView(Cupom c) {
    return new CupomView(
        c.getId(),
        c.getCodigo(),
        c.getTipo(),
        c.getValor(),
        c.getPercentual(),
        c.getSituacao(),
        c.getOrigem());
  }

  private Cupom cupomOpcional(Long id) {
    return id == null ? null : cupons.findById(id).orElse(null);
  }

  private Cliente exigirCliente(Long clienteId) {
    return clientes
        .findById(clienteId)
        .orElseThrow(
            () -> new RegraNegocioException("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado."));
  }

  private static String texto(String v) {
    return v == null || v.isBlank() ? null : v.trim();
  }

  private static BigDecimal zero() {
    return BigDecimal.ZERO.setScale(2);
  }

  // ---- estruturas internas ------------------------------------------------------------------

  private record ItemCalc(Produto produto, int quantidade, BigDecimal subtotal) {}

  private record Desconto(Cupom cupom, BigDecimal valor) {}

  private record ParcelaResolvida(BigDecimal valor, String descricao, Long cartaoId) {}

  private record ResolucaoEndereco(Long enderecoId, EnderecoEntrega snapshot, String estado) {}
}
