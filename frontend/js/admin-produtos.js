import { produtosService as service } from "./produtosService.js";
import { escaparHtml as e, formatarMoeda } from "./utilitarios.js";
import { criarNavegacaoAdmin } from "./adminLayout.js";
import { executar, prepararFormularios } from "./clientesEventos.js";

export const ehRotaProdutosAdmin = () => location.pathname === "/admin/produtos";
let renderVersion = 0;

const emblema = (v, sim, nao) =>
  `<span class="status ${v ? "ok" : ""}">${v ? sim : nao}</span>`;
const status = (v) => emblema(v, "ATIVO", "INATIVO");
const visibilidade = (v) => emblema(v, "VISÍVEL", "OCULTO");
const layout = (conteudo) =>
  `<main class="admin clientes-admin"><div>${criarNavegacaoAdmin("Produtos", "data-api-link")}<section class="admin-main"><div class="admin-topbar"><div><span class="eyebrow">Administração</span><h1>Produtos</h1></div><span class="status ok">ADMIN_DEMO</span></div><div data-testid="produtos-conteudo">${conteudo}</div></section></div></main>`;
const objetoForm = (f) => Object.fromEntries(new FormData(f));
const valor = (d, k) => e(d?.[k] ?? "");

function ligarLinks(navegar) {
  document.querySelectorAll("[data-api-link]").forEach(
    (a) =>
      (a.onclick = (ev) => {
        ev.preventDefault();
        navegar(a.getAttribute("href"));
      }),
  );
}
function erroHtml(err) {
  const campos = Object.entries(err.campos || {})
    .map(([k, v]) => `${e(k)}: ${e(v)}`)
    .join(" · ");
  return `<div class="alert alert-danger" data-testid="erro-api" role="alert">${e(err.message)}${campos ? `<small>${campos}</small>` : ""}</div>`;
}

async function lista(raiz, navegar, toast) {
  const rota = location.href,
    versao = ++renderVersion;
  const q = new URLSearchParams(location.search),
    f = {};
  ["codigo", "nome", "ativo"].forEach((k) => {
    const v = q.get(k);
    if (v) f[k] = v;
  });
  const produtos = await service.listar(f);
  const novo = q.get("new") === "1";
  const formulario = novo
    ? `<form class="panel form-stack" data-testid="form-produto"><h2>Cadastrar produto</h2><div class="row g-3"><div class="col-md-5"><label>Nome<input class="form-control" name="nome" data-testid="nome" required></label></div><div class="col-md-3"><label>Marca<input class="form-control" name="marca" required></label></div><div class="col-md-2"><label>Preço<input class="form-control" type="number" name="preco" min="0" step="0.01" required></label></div><div class="col-md-2"><label>Estoque<input class="form-control" type="number" name="estoque" min="0" required></label></div></div><label><input type="checkbox" name="visivel" checked> Publicar no catálogo</label><button class="btn aura-btn" type="submit" data-testid="salvar-produto">Salvar produto</button></form>`
    : "";
  if (rota !== location.href || versao !== renderVersion) return;
  const linhas =
    produtos
      .map(
        (p) =>
          `<tr data-testid="produto-${p.id}"><td>${e(p.codigo)}</td><td><b>${e(p.nome)}</b><small>${e(p.marca)}</small></td><td>${formatarMoeda(p.preco)}</td><td>${p.estoque}</td><td>${status(p.ativo)}</td><td>${visibilidade(p.visivel)}</td><td><div class="table-actions">${p.ativo ? `<a href="/admin/produtos?motivo=${p.id}&acao=inativar" data-api-link data-inativar-produto="${p.id}">Inativar</a>` : `<a href="/admin/produtos?motivo=${p.id}&acao=ativar" data-api-link data-ativar-produto="${p.id}">Ativar</a>`}</div></td></tr>`,
      )
      .join("") ||
    '<tr><td colspan="7" data-testid="sem-resultados">Nenhum produto encontrado.</td></tr>';
  raiz.innerHTML = layout(
    `<div class="toolbar"><p class="muted">Ativar ou inativar produto exige categoria e justificativa, registradas em auditoria.</p><a class="btn aura-btn" href="/admin/produtos?new=1" data-api-link>Cadastrar novo produto</a></div>${formulario}<form class="panel filter-bar mt-3" data-testid="filtros-produtos"><input class="form-control" name="codigo" placeholder="Código" value="${valor(f, "codigo")}"><input class="form-control" name="nome" placeholder="Nome" value="${valor(f, "nome")}"><select class="form-select" name="ativo"><option value="">Ativos e inativos</option><option value="true" ${f.ativo === "true" ? "selected" : ""}>Ativos</option><option value="false" ${f.ativo === "false" ? "selected" : ""}>Inativos</option></select><button class="btn aura-btn">Filtrar</button></form><div class="panel table-responsive mt-3"><table data-testid="tabela-produtos"><thead><tr><th>Código</th><th>Produto</th><th>Preço</th><th>Estoque</th><th>Situação</th><th>Visibilidade</th><th>Ações</th></tr></thead><tbody>${linhas}</tbody></table></div>`,
  );
  prepararFormularios(raiz);
  ligarLinks(navegar);
  raiz.querySelector("[data-testid=filtros-produtos]").onsubmit = (ev) => {
    ev.preventDefault();
    const d = objetoForm(ev.currentTarget),
      p = new URLSearchParams();
    Object.entries(d).forEach(([k, v]) => v && p.set(k, v));
    navegar(`/admin/produtos?${p}`);
  };
  raiz.querySelector("[data-testid=form-produto]")?.addEventListener("submit", (ev) => {
    ev.preventDefault();
    const d = objetoForm(ev.currentTarget);
    executar(
      ev.currentTarget,
      () =>
        service.cadastrar({
          nome: d.nome,
          marca: d.marca,
          preco: Number(d.preco),
          estoque: Number(d.estoque),
          visivel: d.visivel === "on",
        }),
      toast,
      () => navegar("/admin/produtos"),
    );
  });
}

async function motivo(raiz, navegar, toast, id, acao) {
  if (acao !== "ativar" && acao !== "inativar") return lista(raiz, navegar, toast);
  const rota = location.href,
    versao = ++renderVersion;
  const [produto, categorias] = await Promise.all([
    service.buscar(id),
    acao === "ativar" ? service.categoriasAtivacao() : service.categoriasInativacao(),
  ]);
  if (rota !== location.href || versao !== renderVersion) return;
  const ativar = acao === "ativar";
  const rotulo = ativar ? "ativação" : "inativação";
  raiz.innerHTML = layout(
    `<div class="toolbar"><a href="/admin/produtos" data-api-link>← Voltar para produtos</a></div><form class="panel form-stack" data-testid="form-motivo"><h2>${ativar ? "Ativar" : "Inativar"} produto</h2><p><b>${e(produto.codigo)}</b> · ${e(produto.nome)} · ${e(produto.marca)} · ${formatarMoeda(produto.preco)}</p><p class="muted">A ${rotulo} exige categoria e justificativa, que ficam registradas em auditoria junto da situação anterior.</p><label>Categoria de ${rotulo}</label><select class="form-select" name="categoria" required><option value="" disabled selected>Selecione…</option>${categorias.map((c) => `<option value="${e(c.codigo)}">${e(c.descricao)}</option>`).join("")}</select><label>Justificativa</label><textarea class="form-control" name="justificativa" rows="3" required></textarea><div class="d-flex gap-2"><button class="btn aura-btn" type="submit" data-testid="confirmar-motivo">Confirmar ${rotulo}</button><a class="btn btn-outline-secondary" href="/admin/produtos" data-api-link data-testid="cancelar-motivo">Cancelar</a></div></form>`,
  );
  prepararFormularios(raiz);
  ligarLinks(navegar);
  raiz.querySelector("[data-testid=form-motivo]").addEventListener("submit", (ev) => {
    ev.preventDefault();
    const d = objetoForm(ev.currentTarget),
      dados = { categoria: d.categoria, justificativa: d.justificativa };
    executar(
      ev.currentTarget,
      () => (ativar ? service.ativar(id, dados) : service.inativar(id, dados)),
      toast,
      () => navegar("/admin/produtos"),
    );
  });
}

export async function renderizarProdutosAdmin(raiz, navegar, toast) {
  const rotaInicial = location.href;
  const versaoInicial = renderVersion + 1;
  raiz.innerHTML = layout(
    '<div class="panel" data-testid="carregando-produtos">Carregando produtos…</div>',
  );
  try {
    const q = new URLSearchParams(location.search),
      motivoId = q.get("motivo");
    if (motivoId) await motivo(raiz, navegar, toast, motivoId, q.get("acao") || "");
    else await lista(raiz, navegar, toast);
  } catch (err) {
    if (rotaInicial !== location.href || versaoInicial !== renderVersion) return;
    raiz.innerHTML = layout(erroHtml(err));
    prepararFormularios(raiz);
    ligarLinks(navegar);
  }
}
