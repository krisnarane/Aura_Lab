import { criarNavegacaoAdmin } from "./adminLayout.js";
import { escaparHtml as e, formatarMoeda } from "./utilitarios.js";
import { caixasCategorias, categoriasMarcadas } from "./categoriasSelecao.js";

export const ehRotaAnaliseVendas = () => location.pathname === "/admin/dashboard";
const CORES = ["#9f365c", "#167c80", "#8764aa", "#bc6c25", "#4064ad", "#60904a", "#b94778", "#555a77"];
let versao = 0;
let consultaVersao = 0;
let perfil = "ADMIN_DEMO";
let filtros = null;

const layout = (conteudo) => `<main class="admin clientes-admin"><div>${criarNavegacaoAdmin("Dashboard", "data-api-link")}<section class="admin-main"><div class="admin-topbar"><div><span class="eyebrow">Administração</span><h1>Histórico de vendas por categoria</h1></div></div><div class="panel" data-testid="seletor-perfil"><strong>Perfil demonstrativo</strong> <button type="button" class="btn ${perfil === "ADMIN_DEMO" ? "aura-btn" : "btn-outline-secondary"}" data-perfil="ADMIN_DEMO">ADMIN_DEMO</button> <button type="button" class="btn ${perfil === "CLIENTE_DEMO" ? "aura-btn" : "btn-outline-secondary"}" data-perfil="CLIENTE_DEMO">CLIENTE_DEMO</button></div>${conteudo}</section></div></main>`;

// RN0072: mede o período em meses cobertos, igual ao agrupamento mensal do RN0071.
const MINIMO_MESES = 1;
const MAXIMO_MESES = 24;
function mesesCobertos(inicio, fim) {
  const [anoInicio, mesInicio] = inicio.split("-").map(Number);
  const [anoFim, mesFim] = fim.split("-").map(Number);
  return (anoFim - anoInicio) * 12 + (mesFim - mesInicio) + 1;
}
function periodoInvalido(inicio, fim) {
  if (!inicio || !fim) return "Informe a data de início e a data de fim.";
  if (fim < inicio) return "A data de fim não pode ser anterior à data de início.";
  const meses = mesesCobertos(inicio, fim);
  return meses < MINIMO_MESES || meses > MAXIMO_MESES
    ? `O período de análise deve cobrir de ${MINIMO_MESES} a ${MAXIMO_MESES} meses.`
    : null;
}

async function buscar(url, options = {}) {
  const res = await fetch(url, options);
  if (!res.ok) {
    const erro = await res.json().catch(() => ({}));
    throw new Error(erro.mensagem || `Falha HTTP ${res.status}`);
  }
  return res.json();
}

function desenhar(resultado) {
  const series = resultado.series || [];
  const meses = resultado.meses || [];
  const valores = series.flatMap((s) => s.valores.map(Number));
  const max = Math.max(1, ...valores);
  const largura = Math.max(600, meses.length * 85), altura = 320;
  const x = (i) => 55 + i * (largura - 85) / Math.max(1, meses.length - 1);
  const y = (v) => 270 - Number(v) / max * 220;
  const rotulo = (mes) => { const [ano, m] = mes.split("-"); return `${m}/${ano}`; };
  const linhas = series.map((s, n) => {
    const cor = CORES[n % CORES.length];
    const pontos = s.valores.map((v, i) => `${x(i)},${y(v)}`).join(" ");
    return `<g data-testid="serie-${e(s.categoria)}"><polyline fill="none" stroke="${cor}" stroke-width="3" points="${pontos}"/>${s.valores.map((v, i) => `<circle cx="${x(i)}" cy="${y(v)}" r="6" fill="${cor}" tabindex="0" role="img" aria-label="${e(s.descricao)}, ${rotulo(meses[i])}: ${e(formatarMoeda(v))}" data-testid="ponto-${e(s.categoria)}-${meses[i]}"><title>${e(s.descricao)}, ${rotulo(meses[i])}: ${e(formatarMoeda(v))}</title></circle>`).join("")}</g>`;
  }).join("");
  const grafico = `<div style="overflow-x:auto"><svg data-testid="grafico-vendas" role="img" aria-label="Gráfico de linhas do histórico mensal de vendas por categoria" viewBox="0 0 ${largura} ${altura}" style="min-width:${largura}px;width:100%;height:320px">${[0, .25, .5, .75, 1].map(t => `<line x1="50" x2="${largura - 25}" y1="${y(max*t)}" y2="${y(max*t)}" stroke="#ddd"/><text x="2" y="${y(max*t) + 4}" font-size="11">${e(formatarMoeda(max*t))}</text>`).join("")}${linhas}${meses.map((m, i) => `<text x="${x(i)}" y="300" text-anchor="middle" font-size="12">${rotulo(m)}</text>`).join("")}</svg></div>`;
  const legenda = `<div data-testid="legenda-vendas" style="display:flex;gap:16px;flex-wrap:wrap">${series.map((s, i) => `<span><span aria-hidden="true" style="display:inline-block;width:12px;height:12px;background:${CORES[i % CORES.length]};border-radius:50%"></span> ${e(s.descricao)}</span>`).join("")}</div>`;
  const tabela = `<table class="table" data-testid="tabela-vendas"><caption>Valores mensais em reais por categoria</caption><thead><tr><th>Mês/ano</th>${series.map(s => `<th>${e(s.descricao)}</th>`).join("")}</tr></thead><tbody>${meses.map((m, i) => `<tr><th>${rotulo(m)}</th>${series.map(s => `<td data-testid="valor-${e(s.categoria)}-${m}">${e(formatarMoeda(s.valores[i]))}</td>`).join("")}</tr>`).join("")}</tbody></table>`;
  // RNF0045: a legenda identifica nome e cor de cada categoria na parte inferior do gráfico.
  return `<div class="panel mt-3">${grafico}${legenda}<div class="table-responsive">${tabela}</div></div>`;
}

export async function renderizarAnaliseVendas(raiz, navegar) {
  const atual = ++versao;
  const partes = Object.fromEntries(new Intl.DateTimeFormat("en-US", {
    timeZone: "America/Sao_Paulo", year: "numeric", month: "2-digit", day: "2-digit",
  }).formatToParts(new Date()).map(p => [p.type, p.value]));
  const fim = `${partes.year}-${partes.month}-${partes.day}`;
  const inicioPadrao = `${Number(partes.year) - 1}-${partes.month}-01`;
  filtros ||= { inicio: inicioPadrao, fim, categorias: [] };
  const aplicar = (conteudo) => {
    if (atual !== versao || !ehRotaAnaliseVendas()) return;
    raiz.innerHTML = layout(conteudo);
    raiz.querySelectorAll("[data-api-link]").forEach(a => a.onclick = ev => { ev.preventDefault(); navegar(a.getAttribute("href")); });
    raiz.querySelectorAll("[data-perfil]").forEach(b => b.onclick = () => { perfil = b.dataset.perfil; renderizarAnaliseVendas(raiz, navegar); });
    raiz.querySelector("#analise-form")?.addEventListener("submit", async ev => {
      ev.preventDefault();
      const form = ev.currentTarget;
      filtros = { inicio: form.elements.inicio.value, fim: form.elements.fim.value,
        categorias: categoriasMarcadas(form) };
      const aviso = periodoInvalido(filtros.inicio, filtros.fim)
        || (filtros.categorias.length ? null : "Selecione ao menos uma categoria.");
      if (aviso) { raiz.querySelector("#analise-resultado").innerHTML = `<div role="alert" data-testid="aviso-filtros">${e(aviso)}</div>`; return; }
      await consultar();
    });
  };
  if (perfil !== "ADMIN_DEMO") {
    aplicar('<div class="panel mt-3" role="alert" data-testid="acesso-negado">Acesso negado: a análise de vendas é exclusiva do perfil administrador.</div>');
    return;
  }
  try {
    const categorias = await buscar("/api/v1/categorias-produto");
    if (atual !== versao) return;
    if (!filtros.categorias.length) filtros.categorias = categorias.map(c => c.codigo);
    aplicar(`<form class="panel mt-3" id="analise-form" data-testid="filtros-analise"><div class="row g-3"><div class="col-md-4"><label>Data de início<input class="form-control" type="date" name="inicio" value="${e(filtros.inicio)}" required></label></div><div class="col-md-4"><label>Data de fim<input class="form-control" type="date" name="fim" value="${e(filtros.fim)}" required></label></div><div class="col-md-4 d-flex align-items-end"><button class="btn aura-btn" type="submit">Consultar</button></div><div class="col-12">${caixasCategorias(categorias, filtros.categorias, { legenda: "Categorias comparadas" })}</div></div></form><div id="analise-resultado" aria-live="polite"></div>`);
    await consultar();
  } catch (erro) { aplicar(`<div class="alert alert-danger mt-3" role="alert">${e(erro.message)}</div>`); }

  async function consultar() {
    const resultado = raiz.querySelector("#analise-resultado");
    if (!resultado || atual !== versao || perfil !== "ADMIN_DEMO") return;
    const requisicao = ++consultaVersao;
    resultado.innerHTML = '<p>Consultando vendas…</p>';
    const params = new URLSearchParams({ inicio: filtros.inicio, fim: filtros.fim });
    filtros.categorias.forEach(c => params.append("categorias", c));
    try {
      const dados = await buscar(`/api/v1/admin/analise-vendas?${params}`, { headers: { "X-Demo-Perfil": perfil } });
      if (atual === versao && requisicao === consultaVersao && perfil === "ADMIN_DEMO") resultado.innerHTML = desenhar(dados);
    } catch (erro) { if (atual === versao && requisicao === consultaVersao) resultado.innerHTML = `<div class="alert alert-danger" role="alert">${e(erro.message)}</div>`; }
  }
}
