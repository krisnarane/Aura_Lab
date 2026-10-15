/** Adapter: concentra o contrato HTTP do módulo de vendas (carrinho, resumo e pedidos). */
const BASE = "/api/v1";
async function requisitar(caminho, opcoes = {}) {
  let resposta;
  try {
    resposta = await fetch(BASE + caminho, {
      ...opcoes,
      headers: {
        "Content-Type": "application/json",
        ...(opcoes.headers || {}),
      },
    });
  } catch {
    const e = Error("Não foi possível conectar ao servidor. Tente novamente.");
    e.codigo = "REDE_INDISPONIVEL";
    e.campos = {};
    throw e;
  }
  if (!resposta.ok) {
    let erro = {};
    try {
      erro = await resposta.json();
    } catch {}
    const e = Error(erro.mensagem || "Não foi possível concluir a operação.");
    e.codigo = erro.codigo;
    e.campos = erro.campos || {};
    throw e;
  }
  return resposta.status === 204 ? null : resposta.json();
}

export const vendasApi = {
  carrinho: (id) => requisitar(`/clientes/${id}/carrinho`),
  adicionarItem: (id, d) =>
    requisitar(`/clientes/${id}/carrinho/itens`, {
      method: "POST",
      body: JSON.stringify(d),
    }),
  alterarItem: (id, itemId, d) =>
    requisitar(`/clientes/${id}/carrinho/itens/${itemId}`, {
      method: "PUT",
      body: JSON.stringify(d),
    }),
  removerItem: (id, itemId) =>
    requisitar(`/clientes/${id}/carrinho/itens/${itemId}`, { method: "DELETE" }),
  resumo: (id, d) =>
    requisitar(`/clientes/${id}/carrinho/resumo`, {
      method: "POST",
      body: JSON.stringify(d),
    }),
  finalizar: (id, d, chave) =>
    requisitar(`/clientes/${id}/pedidos`, {
      method: "POST",
      headers: chave ? {"Idempotency-Key": chave} : {},
      body: JSON.stringify(d),
    }),
  pedido: (id, pedidoId) => requisitar(`/clientes/${id}/pedidos/${pedidoId}`),
  pedidos: (id) => requisitar(`/clientes/${id}/pedidos`),
  cupons: (id) => requisitar(`/clientes/${id}/cupons`),
  pedidosAdmin: (f = {}) => requisitar(`/admin/pedidos?${new URLSearchParams(f)}`),
  pedidoAdmin: (id) => requisitar(`/admin/pedidos/${id}`),
  validarPagamento: (id, d) => requisitar(`/admin/pedidos/${id}/pagamento/validar`, {method:"POST", body:JSON.stringify(d)}),
  despachar: (id) => requisitar(`/admin/pedidos/${id}/despacho`, {method:"POST"}),
  entregar: (id) => requisitar(`/admin/pedidos/${id}/entrega`, {method:"POST"}),
  solicitarTroca: (cliente, pedido, d, chave) => requisitar(`/clientes/${cliente}/pedidos/${pedido}/trocas`, {method:"POST", headers: {"Idempotency-Key": chave}, body:JSON.stringify(d)}),
  trocas: (cliente, pedido) => requisitar(`/clientes/${cliente}/trocas${pedido ? `?pedidoId=${pedido}` : ""}`),
  trocasAdmin: () => requisitar("/admin/trocas"),
  autorizarTroca: (id) => requisitar(`/admin/trocas/${id}/autorizacao`, {method:"POST"}),
  receberTroca: (id, retornarEstoque) => requisitar(`/admin/trocas/${id}/recebimento`, {method:"POST", body:JSON.stringify({retornarEstoque})}),
  notificacoes: (cliente) => requisitar(`/clientes/${cliente}/notificacoes`),
};
