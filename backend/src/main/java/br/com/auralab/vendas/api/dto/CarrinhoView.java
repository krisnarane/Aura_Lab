package br.com.auralab.vendas.api.dto;

import java.math.BigDecimal;
import java.util.List;

/** Saída do carrinho: itens, subtotal e total de unidades. */
public record CarrinhoView(
    Long id,
    List<ItemCarrinhoView> itens,
    BigDecimal subtotal,
    int totalUnidades,
    java.time.OffsetDateTime venceEm,
    int prazoMinutos,
    List<ItemRetiradoView> retirados,
    List<NotificacaoVendaView> avisos,
    boolean compraPermitida) {}
