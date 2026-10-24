package br.com.auralab.vendas.service;

import br.com.auralab.vendas.api.dto.CheckoutInput;
import br.com.auralab.vendas.api.dto.PedidoView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {
  private final VendaService vendas;
  private final CicloPedidoService ciclo;

  public CheckoutService(VendaService vendas, CicloPedidoService ciclo) {
    this.vendas = vendas;
    this.ciclo = ciclo;
  }

  @Transactional
  public PedidoView finalizar(Long clienteId, CheckoutInput entrada, String chave) {
    PedidoView pedido = vendas.finalizar(clienteId, entrada, chave);
    if (!"EM_PROCESSAMENTO".equals(pedido.status())) return pedido;
    return ciclo.validar(pedido.id());
  }
}
