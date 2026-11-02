/** Consultora integrada ao PostgreSQL e ao Gemini ajustado. RNF0044. */
import { consultoraApi } from "./consultoraApi.js";
import { escaparHtml as e, formatarMoeda as moeda } from "./utilitarios.js";
import { header, footer } from "./pages.js";

const CLIENTE = "aura-lab-perfil-api-cliente";
const NOME = "aura-lab-perfil-api-nome";
let versao = 0;

const mensagemHtml = (m, produtos = []) => `<div class="chat-message ${m.papel === "USER" ? "user-message" : "aura-message"}">
  <b>${m.papel === "USER" ? "Você" : "Aura"}</b><p>${e(m.texto)}</p>
  ${m.papel === "ASSISTANT" ? `<div class="chat-recommendations">${produtos.map((p) =>
    `<article class="aura-card chat-card"><div class="card-body"><b>${e(p.nome)}</b><p>${e(p.descricao)}</p><strong>${moeda(p.preco)}</strong> <a href="/produto/${p.id}" data-chat-link>Ver produto</a></div></article>`).join("")}</div>
    <div class="d-flex gap-2 mt-2"><button type="button" class="btn btn-sm ${m.util === true ? "aura-btn" : "btn-outline-secondary"}" data-avaliar="${m.id}" data-util="true">Útil</button>
    <button type="button" class="btn btn-sm ${m.util === false ? "aura-btn" : "btn-outline-secondary"}" data-avaliar="${m.id}" data-util="false">Não útil</button></div>
    ${m.modeloVersao ? `<small class="muted">Modelo ${e(m.modeloVersao)}</small>` : ""}` : ""}</div>`;

export async function renderizarConsultoraAura(raiz, navegar, toast) {
  const atual = ++versao;
  const cliente = localStorage.getItem(CLIENTE);
  const nome = localStorage.getItem(NOME);
  if (!cliente) {
    navegar("/perfil/dados");
    return;
  }
  raiz.innerHTML = `${header({clienteNome:nome})}<main class="page"><div class="container aura-container page-top"><section class="advisor chat-advisor"><span class="eyebrow">RNF0044</span><h1>Consultora Aura</h1><p>Encontre produtos de acordo com suas compras e preferências. <a href="/perfil/preferencias" data-chat-link>Editar preferências</a></p><div id="aura-chat-messages" class="aura-chat" aria-live="polite"></div><form id="chat-form" class="chat-composer"><input id="advisor-message" class="form-control" maxlength="1000" required placeholder="Pergunte sobre um produto ou peça uma recomendação"><button class="btn aura-btn">Enviar</button></form></section></div></main>${footer()}`;
  ligarLinks(raiz, navegar);
  const area = raiz.querySelector("#aura-chat-messages");
  let conversa = null;
  try {
    const conversas = await consultoraApi.conversas(cliente);
    if (atual !== versao || location.pathname !== "/consultora") return;
    if (conversas.length) {
      conversa = conversas[0].id;
      const mensagens = await consultoraApi.mensagens(cliente, conversa);
      if (atual !== versao || location.pathname !== "/consultora") return;
      for (const m of mensagens) area.insertAdjacentHTML("beforeend", mensagemHtml(m));
      const ids = [...new Set(mensagens.flatMap((m) => m.produtosIds || []))];
      if (ids.length) {
        const catalogo = await Promise.all(ids.map((id) => fetch(`/api/v1/produtos/${id}`).then((r) => r.ok ? r.json() : null).catch(() => null)));
        if (atual !== versao || location.pathname !== "/consultora") return;
        area.innerHTML = mensagens.map((m) => mensagemHtml(m, catalogo.filter((p) => p && m.produtosIds?.includes(p.id) && p.ativo && p.visivel))).join("");
      }
    } else area.innerHTML = `<div class="chat-message aura-message"><b>Aura</b><p>Olá! Como posso ajudar na escolha de um produto?</p></div>`;
  } catch (erro) { toast(erro.message, "error"); }
  ligarLinks(raiz, navegar);
  raiz.querySelector("#chat-form")?.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const form = ev.currentTarget;
    const campo = form.querySelector("#advisor-message");
    const texto = campo.value.trim();
    if (!texto) return;
    form.querySelector("button").disabled = true;
    try {
      const resposta = await consultoraApi.enviar(cliente, conversa, texto);
      conversa = resposta.conversaId;
      area.insertAdjacentHTML("beforeend", mensagemHtml({ papel: "USER", texto }));
      area.insertAdjacentHTML("beforeend", mensagemHtml(resposta.mensagem, resposta.produtos));
      campo.value = "";
      area.scrollTop = area.scrollHeight;
      ligarLinks(raiz, navegar);
    } catch (erro) { toast(erro.message, "error"); }
    finally { form.querySelector("button").disabled = false; }
  });
  area.addEventListener("click", async (ev) => {
    const botao = ev.target.closest("[data-avaliar]");
    if (!botao) return;
    const util = botao.dataset.util === "true";
    const motivo = util ? null : (window.prompt("O que não ajudou? (opcional)", "") ?? "").slice(0, 300);
    try {
      await consultoraApi.avaliar(cliente, botao.dataset.avaliar, util, motivo);
      botao.parentElement.querySelectorAll("button").forEach((b) => b.className = `btn btn-sm ${b === botao ? "aura-btn" : "btn-outline-secondary"}`);
      toast("Avaliação registrada.");
    } catch (erro) { toast(erro.message, "error"); }
  });
}

function ligarLinks(raiz, navegar) {
  raiz.querySelectorAll("[data-chat-link],[data-link]").forEach((a) => a.onclick = (ev) => {
    ev.preventDefault(); navegar(a.getAttribute("href"));
  });
}
