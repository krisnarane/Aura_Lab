package br.com.auralab.produtos.service;

import br.com.auralab.produtos.repository.ProdutoRepository;
import br.com.auralab.vendas.repository.CicloVendaRepository;
import br.com.auralab.vendas.service.ReservaEstoqueService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF0013/RN0016: estoque zero, nunca vendido e preço abaixo do parâmetro. */
@Service
public class InativacaoAutomaticaProdutos {
  private final JdbcTemplate jdbc;
  private final ProdutoRepository produtos;
  private final CicloVendaRepository ciclo;
  private final AuditoriaProdutoService auditoria;
  private final ReservaEstoqueService reservas;
  private final Clock clock;
  private final BigDecimal limite;

  public InativacaoAutomaticaProdutos(JdbcTemplate jdbc, ProdutoRepository produtos,
      CicloVendaRepository ciclo, AuditoriaProdutoService auditoria, ReservaEstoqueService reservas,
      Clock clock, @Value("${aura.produtos.preco-limite-inativacao:50.00}") BigDecimal limite) {
    this.jdbc = jdbc; this.produtos = produtos; this.ciclo = ciclo;
    this.auditoria = auditoria; this.reservas = reservas; this.clock = clock; this.limite = limite;
  }

  @Transactional
  public List<Long> executar() {
    ciclo.bloquearFluxo();
    List<Long> candidatos = jdbc.queryForList("""
        select p.id from produto p
         where p.ativo = true and p.estoque = 0 and p.preco < ?
           and not exists (select 1 from item_pedido i join pedido v on v.id = i.pedido_id
                            where i.produto_id = p.id and v.status not in ('EM_PROCESSAMENTO','REPROVADA'))
         order by p.id
        """, Long.class, limite);
    var inativados = new ArrayList<Long>();
    for (Long id : candidatos) {
      var p = produtos.buscarParaAtualizacao(id).orElseThrow();
      if (!p.isAtivo() || p.getEstoque() != 0 || p.getPreco().compareTo(limite) >= 0) continue;
      var antes = RegistroProduto.status(p, null, null);
      var agora = OffsetDateTime.now(clock);
      var justificativa = "Inativado automaticamente: estoque zero, sem vendas efetivadas e preço abaixo do limite configurado.";
      p.inativar("FORA_DE_MERCADO", justificativa, agora);
      auditoria.registrarComo(id, "INATIVAR_PRODUTO_AUTOMATICAMENTE", "PRODUTO", id,
          antes, RegistroProduto.status(p, "FORA_DE_MERCADO", justificativa), "SISTEMA", agora);
      inativados.add(id);
    }
    if (!inativados.isEmpty()) {
      produtos.flush();
      reservas.processarVencimentos();
    }
    return inativados;
  }
}
