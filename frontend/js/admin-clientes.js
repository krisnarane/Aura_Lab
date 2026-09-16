import { clientesService as service } from "./clientesService.js";
import { escaparHtml as e, formatarMoeda } from "./utilitarios.js";

export const ehRotaClientesAdmin = () =>
  location.pathname === "/admin/clientes" ||
  location.pathname.startsWith("/admin/clientes/");
let renderVersion = 0;
import {
  status,
  layout,
  valor,
  camposPessoais,
  camposEndereco,
} from "./clientesComponentes.js";
import {
  executar,
  executarBotao,
  prepararFormularios,
} from "./clientesEventos.js";
const objetoForm = (f) => Object.fromEntries(new FormData(f));
const enderecoDe = (d, p = "") => ({
  apelido: d[p + "apelido"],
  tipoResidencia: d[p + "tipoResidencia"],
  tipoLogradouro: d[p + "tipoLogradouro"],
  logradouro: d[p + "logradouro"],
  numero: d[p + "numero"],
  bairro: d[p + "bairro"],
  cep: d[p + "cep"],
  cidade: d[p + "cidade"],
  estado: d[p + "estado"],
  pais: d[p + "pais"],
  observacoes: d[p + "observacoes"] || "",
  complemento: d[p + "complemento"] || "",
  cobranca: d[p + "cobranca"] === "on",
  entrega: d[p + "entrega"] === "on",
  preferencialEntrega: d[p + "preferencialEntrega"] === "on",
});

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
    f = Object.fromEntries(q),
    clientes = await service.listar(f);
  const novo = q.get("new") === "1",
    edit = q.get("edit"),
    atual = edit ? await service.buscar(edit) : null;
  const formulario =
    novo || atual
      ? `<form class="panel form-stack" data-testid="form-cliente"><h2>${atual ? "Alterar cliente" : "Cadastrar cliente"}</h2>${camposPessoais(atual || {}, !atual)}${!atual ? `<hr><h3>Endereço residencial</h3>${camposEndereco({}, "endereco_")}<input type="hidden" name="endereco_cobranca" value="on"><input type="hidden" name="endereco_entrega" value="on"><input type="hidden" name="endereco_preferencialEntrega" value="on">` : ""}<button class="btn aura-btn" type="submit" data-testid="salvar-cliente">Salvar cliente</button></form>`
      : "";
  if (rota !== location.href || versao !== renderVersion) return;
  raiz.innerHTML = layout(
    `<div class="toolbar"><p class="muted">Gerencie os dados e o histórico dos clientes.</p><a class="btn aura-btn" href="/admin/clientes?new=1" data-api-link>Cadastrar novo cliente</a></div>${formulario}<form class="panel filter-bar mt-3" data-testid="filtros-clientes"><input class="form-control" name="codigo" placeholder="Código" value="${valor(f, "codigo")}"><input class="form-control" name="nome" placeholder="Nome" value="${valor(f, "nome")}"><input class="form-control" name="cpf" placeholder="CPF" value="${valor(f, "cpf")}"><input class="form-control" name="email" placeholder="E-mail" value="${valor(f, "email")}"><input class="form-control" name="genero" placeholder="Gênero" value="${valor(f, "genero")}"><input class="form-control" type="date" name="nascimento" value="${valor(f, "nascimento")}"><input class="form-control" name="telefoneTipo" placeholder="Tipo telefone" value="${valor(f, "telefoneTipo")}"><input class="form-control" name="telefoneDdd" placeholder="DDD" value="${valor(f, "telefoneDdd")}"><input class="form-control" name="telefoneNumero" placeholder="Telefone" value="${valor(f, "telefoneNumero")}"><select class="form-select" name="ativo"><option value="">Ativos e inativos</option><option value="true" ${f.ativo === "true" ? "selected" : ""}>Ativos</option><option value="false" ${f.ativo === "false" ? "selected" : ""}>Inativos</option></select><button class="btn aura-btn">Filtrar</button></form><div class="panel table-responsive mt-3"><table data-testid="tabela-clientes"><thead><tr><th>Código</th><th>Cliente</th><th>CPF</th><th>Contato</th><th>Ranking</th><th>Situação</th><th>Ações</th></tr></thead><tbody>${clientes.map((c) => `<tr data-testid="cliente-${c.id}"><td>${e(c.codigo)}</td><td><b>${e(c.nome)}</b></td><td>${e(c.cpf)}</td><td>${e(c.email)}<small>(${e(c.telefoneDdd)}) ${e(c.telefoneNumero)}</small></td><td>${c.ranking} / 5</td><td>${status(c.ativo)}</td><td><div class="table-actions"><a href="/admin/clientes/${c.id}" data-api-link>Consultar</a><a href="/admin/clientes?edit=${c.id}" data-api-link>Editar</a>${c.ativo ? `<button data-inativar="${c.id}" class="link-danger">Inativar</button>` : ""}</div></td></tr>`).join("") || '<tr><td colspan="7" data-testid="sem-resultados">Nenhum cliente encontrado.</td></tr>'}</tbody></table></div>`,
  );
  prepararFormularios(raiz);
  ligarLinks(navegar);
  raiz.querySelector("[data-testid=filtros-clientes]").onsubmit = (ev) => {
    ev.preventDefault();
    const d = objetoForm(ev.currentTarget),
      p = new URLSearchParams();
    Object.entries(d).forEach(([k, v]) => v && p.set(k, v));
    navegar(`/admin/clientes?${p}`);
  };
  raiz
    .querySelector("[data-testid=form-cliente]")
    ?.addEventListener("submit", (ev) => {
      ev.preventDefault();
      const d = objetoForm(ev.currentTarget),
        payload = {
          nome: d.nome,
          cpf: d.cpf,
          email: d.email,
          genero: d.genero,
          nascimento: d.nascimento,
          telefoneTipo: d.telefoneTipo,
          telefoneDdd: d.telefoneDdd,
          telefoneNumero: d.telefoneNumero,
        };
      if (!atual) {
        payload.senha = d.senha;
        payload.confirmacaoSenha = d.confirmacaoSenha;
        payload.enderecoResidencial = enderecoDe(d, "endereco_");
      }
      executar(
        ev.currentTarget,
        () =>
          atual
            ? service.alterar(atual.id, payload)
            : service.cadastrar(payload),
        toast,
        () => {
          navegar("/admin/clientes");
        },
      );
    });
  raiz.querySelectorAll("[data-inativar]").forEach(
    (b) =>
      (b.onclick = async () => {
        if (confirm("Inativar este cliente? O histórico será preservado.")) {
          await executarBotao(
            b,
            () => service.inativar(b.dataset.inativar),
            toast,
            () => lista(raiz, navegar, toast),
          );
        }
      }),
  );
}

async function detalhe(raiz, id, navegar, toast) {
  const rota = location.href,
    versao = ++renderVersion;
  const [c, transacoes, auditoria, bandeiras] = await Promise.all([
    service.buscar(id),
    service.transacoes(id),
    service.auditoria(id),
    service.bandeiras(),
  ]);
  const enderecoEmEdicao = c.enderecos.find(
    (x) =>
      String(x.id) === new URLSearchParams(location.search).get("editEndereco"),
  );
  const enderecoRows = c.enderecos
    .map(
      (x) =>
        `<div class="panel mt-2" data-testid="endereco-${x.id}"><b>${e(x.apelido)}</b> · ${x.cobranca ? "Cobrança " : ""}${x.entrega ? "Entrega" : ""}${x.preferencialEntrega ? " · Preferencial" : ""}<p>${e(x.tipoLogradouro)} ${e(x.logradouro)}, ${e(x.numero)} ${e(x.complemento || "")} — ${e(x.bairro)}, ${e(x.cidade)}/${e(x.estado)} · ${e(x.cep)} · ${e(x.pais)}</p><p>${e(x.observacoes || "")}</p>${x.ativo ? `<a href="/admin/clientes/${id}?editEndereco=${x.id}" data-api-link>Editar</a> <button class="link-danger" data-inativar-endereco="${x.id}">Inativar</button>` : "<span>Inativo</span>"}</div>`,
    )
    .join("");
  const cardRows = c.cartoes
    .map(
      (x) =>
        `<div class="panel mt-2" data-testid="cartao-${x.id}"><b>${e(x.bandeira)} final ${e(x.ultimosQuatro)}</b><p>${e(x.titular)} ${x.preferencial ? "· Preferencial" : ""}</p>${x.ativo ? `<button data-preferir-cartao="${x.id}">Preferir</button> <button class="link-danger" data-inativar-cartao="${x.id}">Inativar</button>` : "<span>Inativo</span>"}</div>`,
    )
    .join("");
  if (rota !== location.href || versao !== renderVersion) return;
  raiz.innerHTML = layout(
    `<div class="toolbar"><a href="/admin/clientes" data-api-link>← Voltar</a><button class="btn btn-outline-danger" data-inativar-cliente>Inativar cliente</button></div><div class="row g-4"><section class="col-lg-7"><div class="panel"><div class="section-heading"><h2>${e(c.nome)}</h2>${status(c.ativo)}</div><p><b>${e(c.codigo)}</b> · CPF ${e(c.cpf)}<br>${e(c.email)} · (${e(c.telefoneDdd)}) ${e(c.telefoneNumero)}<br>${e(c.genero)} · Nascimento: ${e(c.nascimento)} · Telefone ${e(c.telefoneTipo)}<br>Ranking: <b>${c.ranking} / 5</b></p></div><div class="panel mt-3"><h2>Transações</h2>${transacoes.map((t) => `<details class="panel" data-testid="transacao-${t.id}"><summary>${e(t.codigo)} · ${e(t.tipo)} · ${e(t.status)} · ${formatarMoeda(t.valor)}</summary><p>${e(t.detalhes || "Sem detalhes adicionais.")}</p><time>${e(new Date(t.ocorridaEm).toLocaleString("pt-BR"))}</time></details>`).join("") || "<p>Nenhuma transação.</p>"}</div><div class="panel mt-3"><h2>Auditoria</h2>${auditoria.map((a) => `<details data-testid="auditoria-${a.id}"><summary>${e(a.operacao)} · ${e(a.ator)} · ${e(new Date(a.ocorridaEm).toLocaleString("pt-BR"))}</summary><p>${e(a.alteracoes)}</p><pre>Antes: ${e(JSON.stringify(a.dadosAnteriores, null, 2))}\nDepois: ${e(JSON.stringify(a.dadosNovos, null, 2))}</pre></details>`).join("") || "<p>Nenhum registro.</p>"}</div></section><aside class="col-lg-5"><h2>Endereços</h2>${enderecoRows}<form class="panel form-stack mt-3" data-testid="form-endereco"><h3>${enderecoEmEdicao ? "Editar endereço" : "Novo endereço"}</h3>${camposEndereco(enderecoEmEdicao || {})}<label><input type="checkbox" name="cobranca" ${enderecoEmEdicao?.cobranca ? "checked" : ""}> Cobrança</label><label><input type="checkbox" name="entrega" ${enderecoEmEdicao ? (enderecoEmEdicao.entrega ? "checked" : "") : "checked"}> Entrega</label><label><input type="checkbox" name="preferencialEntrega" ${enderecoEmEdicao?.preferencialEntrega ? "checked" : ""}> Preferencial de entrega</label><button class="btn aura-btn">${enderecoEmEdicao ? "Salvar endereço" : "Adicionar endereço"}</button></form><h2 class="mt-4">Cartões</h2>${cardRows}<form class="panel form-stack mt-3" data-testid="form-cartao"><h3>Novo cartão fictício</h3><label>Número<input class="form-control" name="numero" required></label><label>Titular<input class="form-control" name="titular" required></label><label>Bandeira<select class="form-select" name="bandeiraId">${bandeiras.map((b) => `<option value="${b.id}">${e(b.nome)}</option>`).join("")}</select></label><label>Código de segurança<input class="form-control" type="password" name="codigoSeguranca" required></label><label><input type="checkbox" name="preferencial"> Preferencial</label><button class="btn aura-btn">Adicionar cartão</button></form><form class="panel form-stack mt-3" data-testid="form-senha"><h3>Alterar somente senha</h3><label>Nova senha<input class="form-control" type="password" name="novaSenha" required></label><label>Confirmação<input class="form-control" type="password" name="confirmacaoSenha" required></label><button class="btn aura-btn">Alterar senha</button></form></aside></div>`,
  );
  prepararFormularios(raiz);
  ligarLinks(navegar);
  const recarregar = () => detalhe(raiz, id, navegar, toast);
  raiz.querySelector("[data-testid=form-endereco]").onsubmit = (ev) => {
    ev.preventDefault();
    const d = objetoForm(ev.currentTarget);
    executar(
      ev.currentTarget,
      () =>
        enderecoEmEdicao
          ? service.alterarEndereco(id, enderecoEmEdicao.id, enderecoDe(d))
          : service.adicionarEndereco(id, enderecoDe(d)),
      toast,
      recarregar,
    );
  };
  raiz.querySelector("[data-testid=form-cartao]").onsubmit = (ev) => {
    ev.preventDefault();
    const d = objetoForm(ev.currentTarget);
    d.bandeiraId = Number(d.bandeiraId);
    d.preferencial = d.preferencial === "on";
    executar(
      ev.currentTarget,
      () => service.adicionarCartao(id, d),
      toast,
      recarregar,
    );
  };
  raiz.querySelector("[data-testid=form-senha]").onsubmit = (ev) => {
    ev.preventDefault();
    const dados = objetoForm(ev.currentTarget);
    executar(
      ev.currentTarget,
      () => service.alterarSenha(id, dados),
      toast,
      recarregar,
    );
  };
  raiz
    .querySelector("[data-inativar-cliente]")
    ?.addEventListener("click", async (ev) => {
      if (confirm("Inativar este cliente? O histórico será preservado.")) {
        await executarBotao(
          ev.currentTarget,
          () => service.inativar(id),
          toast,
          recarregar,
        );
      }
    });
  for (const [attr, acao] of [
    ["inativar-endereco", service.inativarEndereco],
    ["preferir-cartao", service.preferirCartao],
    ["inativar-cartao", service.inativarCartao],
  ]) {
    raiz
      .querySelectorAll(`[data-${attr}]`)
      .forEach(
        (b) =>
          (b.onclick = () =>
            executarBotao(
              b,
              () => acao(id, b.getAttribute(`data-${attr}`)),
              toast,
              recarregar,
            )),
      );
  }
}
export async function renderizarClientesAdmin(raiz, navegar, toast) {
  const rotaInicial = location.href;
  const versaoInicial = renderVersion + 1;
  raiz.innerHTML = layout(
    '<div class="panel" data-testid="carregando-clientes">Carregando clientes…</div>',
  );
  try {
    const partes = location.pathname.split("/").filter(Boolean);
    if (partes[2]) await detalhe(raiz, partes[2], navegar, toast);
    else await lista(raiz, navegar, toast);
  } catch (err) {
    if (rotaInicial !== location.href || versaoInicial !== renderVersion) return;
    raiz.innerHTML = layout(erroHtml(err));
    prepararFormularios(raiz);
    ligarLinks(navegar);
  }
}
