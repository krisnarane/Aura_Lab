/** Integra os painéis existentes de pedidos e trocas ao ciclo persistido do UC05. */
import { clientesService } from "./clientesService.js";
import { vendasService as service } from "./vendasService.js";
import { criarNavegacaoAdmin } from "./adminLayout.js";
import { header, footer } from "./pages.js";
import { escaparHtml as e, formatarMoeda as moeda } from "./utilitarios.js";
import { prepararFormularios, executar, executarBotao } from "./clientesEventos.js";

export const ehRotaPedidosApi = () => /^\/admin\/(pedidos|trocas)(\/|$)/.test(location.pathname) || /^\/perfil\/pedidos\/[^/]+$/.test(location.pathname);
let versao = 0;
const selo = (s) => `<span class="status" data-testid="status-venda">${e(s.replaceAll("_", " "))}</span>`;
const layout = (titulo, conteudo, admin, contexto = {clienteNome:null,totalItens:0}) => admin
  ? `<main class="admin"><div>${criarNavegacaoAdmin(titulo, "data-api-link")}<section class="admin-main"><div class="admin-topbar"><h1>${e(titulo)}</h1><span class="status ok">ADMIN_DEMO</span></div>${conteudo}</section></div></main>`
  : `${header(contexto)}<main class="page"><div class="container aura-container page-top"><h1>${e(titulo)}</h1><nav class="profile-tabs"><a href="/perfil/pedidos" data-api-link>Pedidos</a><a href="/perfil/cupons" data-api-link>Cupons</a></nav>${conteudo}</div></main>${footer()}`;
const acoesPedido = (p) => `<div class="table-actions">${p.acoesPermitidas.includes("DESPACHAR") ? `<button data-despachar="${p.id}">Despachar</button>` : ""}${p.acoesPermitidas.includes("CONFIRMAR_ENTREGA") ? `<button data-entregar="${p.id}">Confirmar entrega</button>` : ""}</div>`;
const trocasHtml = (trocas, admin) => `<div class="panel table-responsive"><h2>${admin ? "Trocas" : "Minhas trocas"}</h2><table><thead><tr><th>Troca / pedido</th><th>Itens</th><th>Situação</th><th>${admin ? "Ações" : "Crédito"}</th></tr></thead><tbody>${trocas.map(t => `<tr data-testid="troca-${t.id}"><td>${e(t.codigo)}<small>Pedido ${t.pedidoId} · ${t.integral ? "Integral" : "Parcial"}</small><small>${e(t.motivo)}</small></td><td>${t.itens.map(i => `${e(i.produtoNome)} × ${i.quantidade}`).join("<br>")}</td><td>${selo(t.status)}</td><td>${admin && t.acoesPermitidas.includes("AUTORIZAR") ? `<button data-autorizar="${t.id}">Autorizar troca</button>` : ""}${admin && t.acoesPermitidas.includes("RECEBER") ? `<form data-testid="receber-troca" data-troca="${t.id}"><button>Confirmar recebimento e reentrada no estoque</button></form>` : ""}${t.cupom ? `${e(t.cupom.codigo)} · ${moeda(t.cupom.valor)}` : ""}</td></tr>`).join("") || '<tr><td colspan="4">Nenhuma troca.</td></tr>'}</tbody></table></div>`;
const detalhe = (p, admin, trocas, notificacoes) => `<div class="row g-4"><section class="col-lg-8"><div class="panel" data-testid="detalhe-pedido"><div class="section-heading"><h2>${e(p.codigo)}</h2>${selo(p.status)}</div>${p.legado ? '<p class="alert-soft">Registro legado: somente consulta; conciliação pendente.</p>' : ""}${p.motivoResultado ? `<p>${e(p.motivoResultado)}</p>` : ""}<p>Cliente ${p.clienteId} · Pagamento ${e(p.pagamentoStatus)}</p>${p.itens.map(i => `<p class="payment-line"><span>${e(i.nome)} × ${i.quantidade}</span><b>${moeda(i.subtotal)}</b></p>`).join("")}<p class="payment-line">Frete <b>${moeda(p.frete)}</b></p><p class="payment-line">Total <b>${moeda(p.total)}</b></p>${p.venceEm && p.status === "EM_PROCESSAMENTO" ? `<p>Reserva válida até ${e(new Date(p.venceEm).toLocaleString("pt-BR"))}.</p>` : ""}${admin ? acoesPedido(p) : ""}</div><div class="panel mt-3"><h2>Composição do pagamento</h2>${p.pagamentos.map(v => `<p class="payment-line"><span>${e(v.descricao || v.tipo)}</span><b>${moeda(v.valor)}</b></p>`).join("")}</div>${!admin && p.acoesPermitidas.includes("SOLICITAR_TROCA") ? `<form class="panel form-stack mt-3" data-testid="solicitar-troca"><h2>Solicitar troca</h2><label>Itens<select class="form-select" name="itemPedidoId"><option value="integral">Toda a compra</option>${p.itens.filter(i => i.quantidade > i.quantidadeJaSolicitada).map(i => `<option value="${i.id}">${e(i.nome)} (${i.quantidade - i.quantidadeJaSolicitada} disponíveis para troca)</option>`).join("")}</select></label><label>Quantidade para troca parcial<input name="quantidade" class="form-control" type="number" min="1" value="1"></label><label>Motivo<textarea class="form-control" name="motivo" maxlength="500" required></textarea></label><button class="btn aura-btn">Solicitar troca</button></form>` : ""}${trocasHtml(trocas, admin)}</section><aside class="col-lg-4"><div class="panel"><h3>Histórico</h3>${p.historico.map(h => `<p>${e(h.status || h.operacao)}<small>${e(new Date(h.data).toLocaleString("pt-BR"))} · ${e(h.ator)}</small></p>`).join("")}</div>${notificacoes.length ? `<div class="panel mt-3"><h3>Notificações</h3>${notificacoes.map(n => `<p>${e(n.mensagem)}</p>`).join("")}</div>` : ""}</aside></div>`;

export async function renderizarPedidosApi(raiz, navegar, toast) {
  const atual = ++versao, rota = location.href, caminho = location.pathname;
  const admin = caminho.startsWith("/admin/"), cliente = localStorage.getItem("aura-lab-perfil-api-cliente");
  try {
    let conteudo, titulo = "Pedidos", pedido, contexto = {clienteNome:null,totalItens:0};
    if (!admin && cliente) {const c = await clientesService.buscar(cliente);contexto={clienteNome:c.nome,totalItens:c.ativo ? (await service.carrinho(cliente)).totalUnidades : null};}
    if (caminho === "/admin/trocas") {
      titulo = "Trocas"; conteudo = trocasHtml(await service.trocasAdmin(), true);
    } else if (caminho === "/admin/pedidos") {
      const filtro = Object.fromEntries(new URLSearchParams(location.search));
      const lista = await service.pedidosAdmin(filtro);
      conteudo = `<form class="panel filter-bar" data-testid="filtros-pedidos"><input class="form-control" name="codigo" placeholder="Número do pedido" value="${e(filtro.codigo || "")}"><input class="form-control" name="clienteId" type="number" placeholder="Cliente" value="${e(filtro.clienteId || "")}"><select class="form-select" name="status"><option value="">Todos os status</option>${["EM_PROCESSAMENTO","APROVADA","REPROVADA","EM_TRANSPORTE","ENTREGUE","EM_TROCA","TROCA_AUTORIZADA","TROCADO"].map(s => `<option value="${s}" ${filtro.status === s ? "selected" : ""}>${s.replaceAll("_", " ")}</option>`).join("")}</select><button class="btn aura-btn">Filtrar</button></form><div class="panel table-responsive mt-3"><table><thead><tr><th>Pedido</th><th>Cliente</th><th>Total</th><th>Status</th><th>Ações válidas</th></tr></thead><tbody>${lista.map(p => `<tr data-testid="pedido-${p.id}"><td><a href="/admin/pedidos/${p.id}" data-api-link>${e(p.codigo)}</a></td><td>${p.clienteId}</td><td>${moeda(p.total)}</td><td>${selo(p.status)}</td><td>${acoesPedido(p)}</td></tr>`).join("") || '<tr><td colspan="5">Nenhum pedido.</td></tr>'}</tbody></table></div>`;
    } else {
      if (!admin && !cliente) { navegar("/perfil/pedidos"); return; }
      const id = caminho.split("/").pop();
      pedido = admin ? await service.pedidoAdmin(id) : await service.pedido(cliente, id);
      const trocas = admin ? (await service.trocasAdmin()).filter(t => t.pedidoId === pedido.id) : await service.trocas(cliente, id);
      conteudo = detalhe(pedido, admin, trocas, admin ? [] : await service.notificacoes(cliente));
    }
    if (atual !== versao || rota !== location.href) return;
    raiz.innerHTML = layout(titulo, conteudo, admin, contexto); prepararFormularios(raiz);
    raiz.querySelectorAll("[data-api-link],[data-link]").forEach(a => a.onclick = ev => {ev.preventDefault(); navegar(a.getAttribute("href"));});
    const recarregar = () => renderizarPedidosApi(raiz, navegar, toast);
    raiz.querySelector('[data-testid="filtros-pedidos"]')?.addEventListener("submit", ev => {ev.preventDefault(); const f = Object.fromEntries(new FormData(ev.currentTarget)); Object.keys(f).forEach(k => {if (!f[k]) delete f[k];}); navegar(`/admin/pedidos?${new URLSearchParams(f)}`);});
    [["despachar",service.despachar],["entregar",service.entregar],["autorizar",service.autorizarTroca]].forEach(([atributo,acao]) => raiz.querySelectorAll(`[data-${atributo}]`).forEach(b => b.onclick = () => executarBotao(b, () => acao(b.dataset[atributo]), toast, recarregar)));
    raiz.querySelectorAll('[data-testid="receber-troca"]').forEach(f => f.onsubmit = ev => {ev.preventDefault(); executar(f, () => service.receberTroca(f.dataset.troca), toast, recarregar);});
    const troca = raiz.querySelector('[data-testid="solicitar-troca"]');
    if (troca) { const chave = crypto.randomUUID(); troca.onsubmit = ev => {ev.preventDefault(); const d = Object.fromEntries(new FormData(troca)), integral = d.itemPedidoId === "integral"; executar(troca, () => service.solicitarTroca(cliente, pedido.id, {integral,itemPedidoId:integral ? null : Number(d.itemPedidoId), quantidade:integral ? null : Number(d.quantidade),motivo:d.motivo},chave),toast,recarregar);}; }
  } catch (err) {
    if (atual === versao && rota === location.href) raiz.innerHTML = layout("Pedidos", `<div class="alert alert-danger" data-testid="erro-api" role="alert">${e(err.message)}</div>`, admin);
  }
}
