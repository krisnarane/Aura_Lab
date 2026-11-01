import { produtosService as service } from "./produtosService.js";
import { escaparHtml as e, formatarMoeda } from "./utilitarios.js";
import { criarNavegacaoAdmin } from "./adminLayout.js";
import { executar, prepararFormularios } from "./clientesEventos.js";
import { aplicarDetalhesVisuais } from "./interface.js";
import { caixasCategorias, categoriasMarcadas } from "./categoriasSelecao.js";

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
  ["codigo", "nome", "marca", "descricao", "categoria", "grupoPrecificacao", "precoMin", "precoMax", "custoMin", "custoMax", "estoqueMin", "estoqueMax", "ativo", "visivel"].forEach((k) => {
    const v = q.get(k);
    if (v) f[k] = v;
  });
  const [produtos, categorias, grupos] = await Promise.all([service.listar(f), service.categoriasProduto(), service.gruposPrecificacao()]);
  const novo = q.get("new") === "1";
  const editarId = q.get("editar");
  const atual = editarId ? produtos.find((p) => String(p.id) === editarId) || await service.buscar(editarId) : null;
  const entradaId = q.get("entradas");
  const produtoEntrada = entradaId ? produtos.find((p) => String(p.id) === entradaId) || await service.buscar(entradaId) : null;
  const historico = entradaId ? await service.entradas(entradaId) : [];
  const formularioEntrada = produtoEntrada
    ? `<form class="panel form-stack" data-testid="form-entrada"><h2>Entrada em estoque · ${e(produtoEntrada.nome)}</h2><p>Estoque: ${produtoEntrada.estoque} · Preço: ${formatarMoeda(produtoEntrada.preco)} · Grupo: ${e(produtoEntrada.grupoPrecificacao.descricao)}</p><label>Quantidade<input class="form-control" type="number" name="quantidade" min="1" required></label><label>Valor de custo<input class="form-control" type="number" name="valorCusto" min="0.01" step="0.01" required></label><label>Data de entrada<input class="form-control" type="date" name="dataEntrada" value="${new Date().toISOString().slice(0, 10)}" required></label><button class="btn aura-btn" type="submit">Registrar entrada</button><h3>Histórico</h3><ul data-testid="historico-entradas">${historico.map((h) => `<li>${e(h.dataEntrada)} · ${h.quantidade} unidade(s) · custo ${formatarMoeda(h.valorCusto)} · preço ${formatarMoeda(h.precoCalculado)}</li>`).join("") || "<li>Nenhuma entrada registrada.</li>"}</ul></form>` : "";
  let formulario = novo || atual
    ? `<form class="panel form-stack" data-testid="form-produto"><h2>${atual ? "Editar" : "Cadastrar"} produto</h2><div class="row g-3"><div class="col-md-5"><label>Nome<input class="form-control" name="nome" data-testid="nome" value="${e(atual?.nome || "")}" required></label></div><div class="col-md-3"><label>Marca<input class="form-control" name="marca" value="${e(atual?.marca || "")}" required></label></div><div class="col-md-2"><label>Preço<input class="form-control" type="number" name="preco" min="0" step="0.01" value="${atual?.preco ?? ""}" required></label></div><div class="col-md-2"><label>Estoque<input class="form-control" type="number" name="estoque" min="0" value="${atual?.estoque ?? ""}" required></label></div></div><label>Descrição<textarea class="form-control" name="descricao" required>${e(atual?.descricao || "")}</textarea></label><label>URL da imagem<input class="form-control" type="url" name="imagemUrl" value="${e(atual?.imagemUrl || "")}" required></label><div class="product-art" data-testid="previa-produto" data-cor-produto="#d7a3ae"><img alt="Prévia do produto" ${atual?.imagemUrl ? `src="${e(atual.imagemUrl)}"` : ""} data-ocultar-imagem-com-erro><span class="product-mark">aura</span></div>${caixasCategorias(categorias, (atual?.categorias || []).map((c) => c.codigo))}<label><input type="checkbox" name="visivel" ${atual?.visivel ?? true ? "checked" : ""}> Publicar no catálogo</label><button class="btn aura-btn" type="submit" data-testid="salvar-produto">Salvar produto</button></form>`
    : "";
  if (formulario) formulario = formulario.replace('<button class="btn aura-btn" type="submit" data-testid="salvar-produto">', `<label>Grupo de precificação<select class="form-select" name="grupoPrecificacao" required><option value="">Selecione…</option>${grupos.map((g) => `<option value="${e(g.codigo)}" ${atual?.grupoPrecificacao?.codigo === g.codigo ? "selected" : ""}>${e(g.descricao)} · ${g.percentualMargem}%</option>`).join("")}</select></label><button class="btn aura-btn" type="submit" data-testid="salvar-produto">`);
  if (rota !== location.href || versao !== renderVersion) return;
  const linhas =
    produtos
      .map(
        (p) =>
          `<tr data-testid="produto-${p.id}"><td>${e(p.codigo)}</td><td><b>${e(p.nome)}</b></td><td>${e(p.marca)}</td><td>${(p.categorias || []).map((c) => e(c.descricao)).join(", ")}</td><td>${e(p.grupoPrecificacao?.descricao)}</td><td>${formatarMoeda(p.preco)}</td><td>${p.estoque}</td><td>${status(p.ativo)}</td><td>${visibilidade(p.visivel)}</td><td><div class="table-actions"><a href="/admin/produtos?editar=${p.id}" data-api-link data-editar-produto="${p.id}">Editar</a> <a href="/admin/produtos?entradas=${p.id}" data-api-link data-entradas-produto="${p.id}">Entradas</a> ${p.ativo ? `<a href="/admin/produtos?motivo=${p.id}&acao=inativar" data-api-link data-inativar-produto="${p.id}">Inativar</a>` : `<a href="/admin/produtos?motivo=${p.id}&acao=ativar" data-api-link data-ativar-produto="${p.id}">Ativar</a>`}</div></td></tr>`,
      )
      .join("") ||
    '<tr><td colspan="10" data-testid="sem-resultados">Nenhum produto encontrado.</td></tr>';
  const filtroCampo = (nome, rotulo, tipo = "text") => `<label>${rotulo}<input class="form-control" type="${tipo}" ${tipo === "number" ? `step="${nome.startsWith("estoque") ? 1 : 0.01}"` : ""} name="${nome}" value="${valor(f, nome)}"></label>`;
  const filtros = `<form class="panel filter-bar mt-3" data-testid="filtros-produtos" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:12px;align-items:end">
    ${filtroCampo("codigo", "Código")}${filtroCampo("nome", "Nome")}${filtroCampo("marca", "Marca")}${filtroCampo("descricao", "Descrição")}
    <label>Categoria<select class="form-select" name="categoria"><option value="">Todas</option>${categorias.map((c) => `<option value="${e(c.codigo)}" ${f.categoria === c.codigo ? "selected" : ""}>${e(c.descricao)}</option>`).join("")}</select></label>
    <label>Grupo<select class="form-select" name="grupoPrecificacao"><option value="">Todos</option>${grupos.map((g) => `<option value="${e(g.codigo)}" ${f.grupoPrecificacao === g.codigo ? "selected" : ""}>${e(g.descricao)}</option>`).join("")}</select></label>
    ${filtroCampo("precoMin", "Preço mínimo", "number")}${filtroCampo("precoMax", "Preço máximo", "number")}
    ${filtroCampo("custoMin", "Custo mínimo", "number")}${filtroCampo("custoMax", "Custo máximo", "number")}
    ${filtroCampo("estoqueMin", "Estoque mínimo", "number")}${filtroCampo("estoqueMax", "Estoque máximo", "number")}
    <label>Situação<select class="form-select" name="ativo"><option value="">Todas</option><option value="true" ${f.ativo === "true" ? "selected" : ""}>Ativos</option><option value="false" ${f.ativo === "false" ? "selected" : ""}>Inativos</option></select></label>
    <label>Visibilidade<select class="form-select" name="visivel"><option value="">Todas</option><option value="true" ${f.visivel === "true" ? "selected" : ""}>Visíveis</option><option value="false" ${f.visivel === "false" ? "selected" : ""}>Ocultos</option></select></label>
    <button class="btn aura-btn">Filtrar</button></form>`;
  raiz.innerHTML = layout(
    `<div class="toolbar"><p class="muted">Ativar ou inativar produto exige categoria e justificativa, registradas em auditoria.</p><a class="btn aura-btn" href="/admin/produtos?new=1" data-api-link>Cadastrar novo produto</a><button class="btn btn-outline-secondary" type="button" data-testid="inativacao-automatica">Executar inativação automática</button></div>${formulario}${formularioEntrada}${filtros}<div class="panel table-responsive mt-3"><table data-testid="tabela-produtos"><thead><tr><th>Código</th><th>Produto</th><th>Marca</th><th>Categorias</th><th>Grupo</th><th>Preço</th><th>Estoque</th><th>Situação</th><th>Visibilidade</th><th>Ações</th></tr></thead><tbody>${linhas}</tbody></table></div>`,
  );
  aplicarDetalhesVisuais(raiz);
  prepararFormularios(raiz);
  ligarLinks(navegar);
  raiz.querySelector('[data-testid="inativacao-automatica"]').onclick = async () => {
    if (!window.confirm("Executar a inativação automática dos produtos elegíveis?")) return;
    try {
      const ids = await service.inativacaoAutomatica();
      toast(`${ids.length} produto(s) inativado(s).`);
      navegar("/admin/produtos");
    } catch (err) { raiz.querySelector('[data-testid="produtos-conteudo"]').insertAdjacentHTML("afterbegin", erroHtml(err)); }
  };
  raiz.querySelector('[data-testid="form-entrada"]')?.addEventListener("submit", (ev) => {
    ev.preventDefault();
    const d = objetoForm(ev.currentTarget);
    executar(ev.currentTarget, () => service.registrarEntrada(entradaId, {
      quantidade: Number(d.quantidade), valorCusto: Number(d.valorCusto), dataEntrada: d.dataEntrada,
    }), toast, () => navegar(`/admin/produtos?entradas=${entradaId}`));
  });
  raiz.querySelector('[name="imagemUrl"]')?.addEventListener("input", (ev) => {
    const img = raiz.querySelector('[data-testid="previa-produto"] img');
    img.hidden = false;
    img.onerror = () => { img.hidden = true; };
    img.src = ev.target.value;
  });
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
    const dados = {
      nome: d.nome,
      marca: d.marca,
      descricao: d.descricao,
      imagemUrl: d.imagemUrl,
      categorias: categoriasMarcadas(ev.currentTarget),
      grupoPrecificacao: d.grupoPrecificacao,
      preco: Number(d.preco),
      estoque: Number(d.estoque),
      visivel: d.visivel === "on",
    };
    executar(
      ev.currentTarget,
      () => {
        // Caixas de seleção não aceitam `required`; o aviso evita depender do 400 da API.
        if (!dados.categorias.length)
          throw new Error("Marque ao menos uma categoria do produto.");
        return atual ? service.editar(atual.id, dados) : service.cadastrar(dados);
      },
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
