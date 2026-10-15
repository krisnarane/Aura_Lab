package br.com.auralab.vendas.service;

import static br.com.auralab.vendas.repository.CicloVendaRepository.*;

import br.com.auralab.clientes.service.AuditoriaService;
import br.com.auralab.vendas.api.dto.*;
import br.com.auralab.vendas.domain.*;
import br.com.auralab.vendas.repository.CicloVendaRepository;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RN0032/RN0044/RN0045: reserva por unidades e sincronização transacional do carrinho. */
@Service
public class ReservaEstoqueService {
  private final CicloVendaRepository repo;
  private final AuditoriaService auditoria;
  private final Clock clock;
  private final Duration prazo;
  @PersistenceContext private EntityManager em;

  public ReservaEstoqueService(
      CicloVendaRepository repo,
      AuditoriaService auditoria,
      Clock clock,
      @Value("${aura.vendas.prazo-reserva:PT20M}") String prazo) {
    this.repo = repo;
    this.auditoria = auditoria;
    this.clock = clock;
    this.prazo = Duration.parse(prazo);
    if (this.prazo.compareTo(Duration.ofMinutes(5)) < 0)
      throw new IllegalArgumentException("Prazo deve comportar o aviso de cinco minutos.");
  }

  public OffsetDateTime agora() {
    return OffsetDateTime.now(clock);
  }

  public int prazoMinutos() {
    return Math.toIntExact(prazo.toMinutes());
  }

  @Transactional
  public void sincronizar(Long cliente) {
    repo.bloquearFluxo();
    em.flush();
    em.clear();
    OffsetDateTime agora = agora();
    conciliarCarrinhoLegado(cliente,agora);
    Set<Long> pedidosExpirados = new HashSet<>();
    for (var r : repo.reservas(cliente)) {
      Long reserva = id(r, "id"), item = id(r, "item_carrinho_id"), pedido = id(r, "pedido_id");
      int quantidade = qtd(r, "quantidade");
      OffsetDateTime fim = tempo(r, "vence_em");
      if (!fim.isAfter(agora)) {
        if (pedido != null) {
          if (pedidosExpirados.add(pedido)) {
            repo.encerrarPedido(pedido, "EXPIRADA");
            repo.reprovarExpirado(pedido);
            var p = repo.pedido(pedido);
            auditoria.registrarComo(
                cliente,
                "SISTEMA",
                agora,
                "EXPIRAR_PEDIDO",
                "PEDIDO",
                pedido,
                Map.of("status", "EM_PROCESSAMENTO"),
                Map.of("status", "REPROVADA", "motivo", "RESERVA_EXPIRADA"));
            repo.transacao(
                cliente,
                "PEDIDO",
                pedido,
                (String) p.get("codigo"),
                "PEDIDO",
                "REPROVADA",
                (BigDecimal) p.get("total"),
                "Pedido real; reserva expirada.",
                agora);
            notificar(
                cliente,
                "pedido-expirado-" + pedido,
                "RESERVA_EXPIRADA",
                "A reserva do pedido " + p.get("codigo") + " expirou; a compra foi reprovada.");
          }
        } else if (item != null) {
          repo.encerrarReserva(reserva, "EXPIRADA");
          repo.retirarItem(
              item,
              id(r, "carrinho_id"),
              id(r, "produto_id"),
              (String) r.get("nome"),
              quantidade,
              "PRAZO_EXPIRADO",
              prazo.toMinutes(),
              agora);
          repo.tocarCarrinho(id(r, "carrinho_id"), agora);
          auditoria.registrarComo(
              cliente,
              "SISTEMA",
              agora,
              "EXPIRAR_ITEM_CARRINHO",
              "CARRINHO",
              item,
              Map.of("produtoId", id(r, "produto_id"), "quantidade", quantidade),
              Map.of("quantidade", 0, "motivo", "PRAZO_EXPIRADO"));
          notificar(
              cliente,
              "expirada-" + reserva,
              "RESERVA_EXPIRADA",
              r.get("nome")
                  + " foi retirado por expiração do prazo de "
                  + prazo.toMinutes()
                  + " minutos. Adicione novamente para comprar.");
        }
        continue;
      }
      if (!agora.isBefore(fim.minusMinutes(5)))
        notificar(
            cliente,
            "aviso-" + reserva + "-" + fim.toInstant(),
            "RESERVA_VENCENDO",
            "A reserva de " + r.get("nome") + " expira em cinco minutos (" + fim + ").");
      if (item != null) {
        int disponivel =
            Boolean.TRUE.equals(r.get("ativo")) && Boolean.TRUE.equals(r.get("visivel"))
                ? repo.disponivel(id(r, "produto_id"), item, agora)
                : 0;
        if (disponivel < quantidade) {
          if (disponivel == 0) {
            repo.encerrarReserva(reserva, "LIBERADA");
            repo.retirarItem(
                item,
                id(r, "carrinho_id"),
                id(r, "produto_id"),
                (String) r.get("nome"),
                quantidade,
                "SEM_ESTOQUE",
                prazo.toMinutes(),
                agora);
          } else {
            repo.ajustarReserva(reserva, disponivel);
            repo.ajustarItem(item, disponivel);
          }
          repo.tocarCarrinho(id(r, "carrinho_id"), agora);
          auditoria.registrarComo(
              cliente,
              "SISTEMA",
              agora,
              "AJUSTAR_DISPONIBILIDADE",
              "CARRINHO",
              item,
              Map.of("quantidade", quantidade),
              Map.of("quantidade", disponivel, "produtoId", id(r, "produto_id")));
          notificar(
              cliente,
              "ajuste-" + reserva + "-" + quantidade + "-" + disponivel,
              "ESTOQUE_ALTERADO",
              "Disponibilidade de "
                  + r.get("nome")
                  + " mudou: "
                  + (disponivel == 0
                      ? "item removido do carrinho."
                      : "quantidade ajustada para " + disponivel + "."));
        }
      }
    }
    em.clear();
  }

  @Transactional
  public void processarVencimentos() {
    for (Long cliente : repo.clientesComReservas()) sincronizar(cliente);
  }

  private void conciliarCarrinhoLegado(Long cliente,OffsetDateTime agora) {
    for(var item:repo.itensLegadosSemReserva(cliente)) {
      var inclusao=tempo(item,"ultima_inclusao");
      var vencimento=inclusao==null?null:inclusao.plus(prazo);
      Long itemId=id(item,"id"),cart=id(item,"carrinho_id"),produto=id(item,"produto_id");
      int quantidade=qtd(item,"quantidade");
      if(vencimento!=null && vencimento.isAfter(agora)) {
        repo.reservar(cliente,cart,itemId,produto,quantidade,agora,vencimento);
        auditoria.registrarComo(cliente,"SISTEMA",agora,"CONCILIAR_CARRINHO_LEGADO","CARRINHO",cart,
          Map.of("produtoId",produto,"quantidade",quantidade,"reservaPersistida",false),
          Map.of("produtoId",produto,"quantidade",quantidade,"venceEm",vencimento.toString()));
      } else {
        String motivo=inclusao==null?"REINCLUSAO_NECESSARIA":"PRAZO_EXPIRADO";
        repo.retirarItem(itemId,cart,produto,(String)item.get("nome"),quantidade,motivo,prazo.toMinutes(),agora);
        repo.tocarCarrinho(cart,agora);
        auditoria.registrarComo(cliente,"SISTEMA",agora,"CONCILIAR_CARRINHO_LEGADO","CARRINHO",cart,
          Map.of("produtoId",produto,"quantidade",quantidade),Map.of("quantidade",0,"motivo",motivo));
        notificar(cliente,"carrinho-legado-"+itemId,"REINCLUSAO_NECESSARIA",item.get("nome")+" foi preservado na lista de itens retirados. Inclua novamente para comprar; motivo: "+motivo+".");
      }
    }
  }

  public int disponivel(Long produto, Long item) {
    return repo.disponivel(produto, item, agora());
  }

  public void adicionar(Carrinho cart, ItemCarrinho item) {
    OffsetDateTime fim = agora().plus(prazo);
    repo.reservar(
        cart.getClienteId(),
        cart.getId(),
        item.getId(),
        item.getProdutoId(),
        item.getQuantidade(),
        agora(),
        fim);
    repo.renovar(cart.getId(), fim);
    repo.marcarReadicionado(cart.getId(), item.getProdutoId());
  }

  public void alterar(Carrinho cart, ItemCarrinho item) {
    OffsetDateTime fim = repo.vencimentoCarrinho(cart.getId());
    repo.reservar(
        cart.getClienteId(),
        cart.getId(),
        item.getId(),
        item.getProdutoId(),
        item.getQuantidade(),
        agora(),
        fim);
  }

  public void remover(Long item) {
    repo.liberarItem(item);
  }

  public void transferir(Carrinho cart, Pedido pedido) {
    for (var item : pedido.getItens())
      repo.transferir(cart.getId(), item.getProdutoId(), item.getId());
  }

  public OffsetDateTime vencimento(Long carrinho) {
    return carrinho == null ? null : repo.vencimentoCarrinho(carrinho);
  }

  public List<ItemRetiradoView> retirados(Long carrinho) {
    if (carrinho == null) return List.of();
    return repo.retirados(carrinho).stream()
        .map(
            r ->
                new ItemRetiradoView(
                    id(r, "produto_id"),
                    (String) r.get("produto_nome"),
                    qtd(r, "quantidade"),
                    (String) r.get("motivo"),
                    id(r, "prazo_minutos"),
                    tempo(r, "retirado_em"),
                    (Boolean) r.get("readicionado")))
        .toList();
  }

  public void notificar(Long cliente, String chave, String tipo, String mensagem) {
    Long id = repo.notificar(cliente, chave, tipo, mensagem, agora());
    if (id != null)
      auditoria.registrarComo(
          cliente,
          "SISTEMA",
          agora(),
          "GERAR_NOTIFICACAO",
          "NOTIFICACAO",
          id,
          null,
          Map.of("tipo", tipo, "mensagem", mensagem));
  }

  public List<NotificacaoVendaView> notificacoes(Long cliente) {
    return mapearNotificacoes(repo.notificacoes(cliente));
  }

  public List<NotificacaoVendaView> avisosCarrinho(Long carrinho) {
    return carrinho == null ? List.of() : mapearNotificacoes(repo.avisosCarrinho(carrinho, agora()));
  }

  private List<NotificacaoVendaView> mapearNotificacoes(List<Map<String, Object>> notificacoes) {
    return notificacoes.stream()
        .map(
            r ->
                new NotificacaoVendaView(
                    id(r, "id"),
                    (String) r.get("tipo"),
                    (String) r.get("mensagem"),
                    tempo(r, "criada_em"),
                    (Boolean) r.get("lida")))
        .toList();
  }
}
