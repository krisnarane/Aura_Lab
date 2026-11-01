package br.com.auralab.produtos.service;

import br.com.auralab.clientes.service.RegraNegocioException;
import br.com.auralab.produtos.api.dto.*;
import br.com.auralab.produtos.domain.EntradaEstoque;
import br.com.auralab.produtos.repository.*;
import br.com.auralab.vendas.repository.CicloVendaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF0051/RF0052: entrada, atualização de estoque/preço e auditoria atômicas. */
@Service
public class EntradaEstoqueService {
  private final ProdutoRepository produtos;
  private final EntradaEstoqueRepository entradas;
  private final CicloVendaRepository ciclo;
  private final AuditoriaProdutoService auditoria;
  private final Clock clock;

  public EntradaEstoqueService(ProdutoRepository produtos, EntradaEstoqueRepository entradas,
      CicloVendaRepository ciclo, AuditoriaProdutoService auditoria, Clock clock) {
    this.produtos = produtos; this.entradas = entradas; this.ciclo = ciclo;
    this.auditoria = auditoria; this.clock = clock;
  }

  @Transactional
  public EntradaEstoqueView registrar(Long id, EntradaEstoqueInput in) {
    if (in.quantidade() == null || in.quantidade() <= 0)
      throw new RegraNegocioException("QUANTIDADE_ENTRADA_INVALIDA", "Quantidade deve ser maior que zero.");
    if (in.valorCusto() == null || in.valorCusto().signum() <= 0)
      throw new RegraNegocioException("VALOR_CUSTO_INVALIDO", "Valor de custo deve ser maior que zero.");
    var custo = in.valorCusto().setScale(2, RoundingMode.HALF_UP);
    if (custo.signum() <= 0)
      throw new RegraNegocioException("VALOR_CUSTO_INVALIDO", "Valor de custo deve ser ao menos R$ 0,01.");
    if (in.dataEntrada() == null)
      throw new RegraNegocioException("DATA_ENTRADA_OBRIGATORIA", "Informe a data de entrada.");
    ciclo.bloquearFluxo();
    var produto = produtos.buscarParaAtualizacao(id).orElseThrow(() ->
        new RegraNegocioException("PRODUTO_NAO_ENCONTRADO", "Produto não encontrado."));
    var antes = RegistroProduto.cadastro(produto);
    var agora = OffsetDateTime.now(clock);
    produto.registrarEntrada(in.quantidade(), custo, agora);
    var entrada = entradas.saveAndFlush(new EntradaEstoque(produto, in.quantidade(), custo,
        in.dataEntrada(), produto.getPreco(), agora));
    auditoria.registrar(id, "ENTRADA_ESTOQUE", "ENTRADA_ESTOQUE", entrada.getId(), antes,
        RegistroProduto.cadastro(produto));
    return view(entrada);
  }

  @Transactional(readOnly = true)
  public List<EntradaEstoqueView> listar(Long id) {
    if (!produtos.existsById(id))
      throw new RegraNegocioException("PRODUTO_NAO_ENCONTRADO", "Produto não encontrado.");
    return entradas.findByProdutoIdOrderByDataEntradaDescIdDesc(id).stream().map(this::view).toList();
  }

  private EntradaEstoqueView view(EntradaEstoque e) {
    return new EntradaEstoqueView(e.getId(), e.getQuantidade(), e.getValorCusto(),
        e.getDataEntrada(), e.getPrecoCalculado(), e.getRegistradaEm());
  }
}
