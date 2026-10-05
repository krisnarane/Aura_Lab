import { vendasApi as api } from "./vendasApi.js";
/** Facade: operações estáveis de vendas consumidas pela loja (carrinho e checkout). */
export const vendasService = {
  carrinho: (id) => api.carrinho(id),
  adicionarItem: (id, d) => api.adicionarItem(id, d),
  alterarItem: (id, itemId, d) => api.alterarItem(id, itemId, d),
  removerItem: (id, itemId) => api.removerItem(id, itemId),
  resumo: (id, d) => api.resumo(id, d),
  finalizar: (id, d) => api.finalizar(id, d),
  pedido: (id, pedidoId) => api.pedido(id, pedidoId),
  pedidos: (id) => api.pedidos(id),
  cupons: (id) => api.cupons(id),
};
