package br.com.auralab.vendas.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Saída do pedido finalizado (RF0038): status EM_PROCESSAMENTO, totais e composição. */
public record PedidoView(
    Long id,
    String codigo,
    String status,
    BigDecimal subtotal,
    BigDecimal desconto,
    BigDecimal frete,
    BigDecimal total,
    EnderecoEntregaView entrega,
    List<ItemPedidoView> itens,
    List<PagamentoParteView> pagamentos,
    CupomView cupomTrocaGerado,
    OffsetDateTime criadoEm) {}
