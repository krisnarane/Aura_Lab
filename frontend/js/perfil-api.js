/** Perfil do cliente integrado à API real: dados, senha, endereços e cartões persistidos no PostgreSQL. */
import { clientesService as service } from "./clientesService.js";
import { escaparHtml as e } from "./utilitarios.js";
import {
  status,
  camposPessoais,
  camposEndereco,
  valor,
} from "./clientesComponentes.js";
import { prepararFormularios, executar } from "./clientesEventos.js";
import { header, footer } from "./pages.js";

const CHAVE_CLIENTE = "aura-lab-perfil-api-cliente";
const CHAVE_NOME = "aura-lab-perfil-api-nome";

export const ehRotaPerfilApi = () =>
  [
    "/perfil/dados",
    "/perfil/seguranca",
    "/perfil/enderecos",
    "/perfil/cartoes",
  ].includes(location.pathname);

let renderVersion = 0;

const lerClienteId = () => localStorage.getItem(CHAVE_CLIENTE);
const definirCliente = (id, nome) => {
  localStorage.setItem(CHAVE_CLIENTE, String(id));
  localStorage.setItem(CHAVE_NOME, nome || "");
};
const limparCliente = () => {
  localStorage.removeItem(CHAVE_CLIENTE);
  localStorage.removeItem(CHAVE_NOME);
};

const objetoForm = (f) => Object.fromEntries(new FormData(f));
const enderecoDe = (d, prefixo = "") => ({
  apelido: d[prefixo + "apelido"],
  tipoResidencia: d[prefixo + "tipoResidencia"],
  tipoLogradouro: d[prefixo + "tipoLogradouro"],
  logradouro: d[prefixo + "logradouro"],
  numero: d[prefixo + "numero"],
  bairro: d[prefixo + "bairro"],
  cep: d[prefixo + "cep"],
  cidade: d[prefixo + "cidade"],
  estado: d[prefixo + "estado"],
  pais: d[prefixo + "pais"],
  observacoes: d[prefixo + "observacoes"] || "",
  complemento: d[prefixo + "complemento"] || "",
  cobranca: d[prefixo + "cobranca"] === "on",
  entrega: d[prefixo + "entrega"] === "on",
  preferencialEntrega: d[prefixo + "preferencialEntrega"] === "on",
});

const NAVEGACAO_PERFIL = [
  ["Dados", "/perfil/dados", "dados"],
  ["Segurança", "/perfil/seguranca", "seguranca"],
  ["Endereços", "/perfil/enderecos", "enderecos"],
  ["Cartões", "/perfil/cartoes", "cartoes"],
];

const layoutPerfil = (ativa, titulo, conteudo) =>
  `${header()}<main class="page"><div class="container aura-container page-top"><span class="eyebrow">Cliente cadastrado</span><h1>${e(titulo)}</h1><nav class="profile-tabs">${NAVEGACAO_PERFIL.map(
    ([label, href, chave]) =>
      `<a href="${href}" data-api-link class="${ativa === chave ? "active" : ""}">${label}</a>`,
  ).join("")}</nav><div class="profile-content wide">${conteudo}</div></div></main>${footer()}`;

function ligarLinks(navegar) {
  document.querySelectorAll("[data-api-link],[data-link]").forEach(
    (a) =>
      (a.onclick = (ev) => {
        ev.preventDefault();
        navegar(a.getAttribute("href"));
      }),
  );
}

const paginaEscolha = (cadastrados, falha) =>
  `${header()}<main class="page"><div class="container aura-container page-top"><span class="eyebrow">Área do cliente</span><h1>Selecione um cliente cadastrado</h1><div class="panel form-stack"><p>As telas de perfil da loja agora leem e gravam diretamente no PostgreSQL. Escolha qual cliente real está navegando nesta sessão.</p>${
    falha
      ? `<div class="alert alert-danger" role="alert">${e(falha)}</div>`
      : cadastrados
          .map(
            (c) =>
              `<button class="btn aura-btn" data-entrar-cliente="${c.id}" data-nome="${e(c.nome)}">Entrar como ${e(c.nome)} · ${e(c.codigo)}</button>`,
          )
          .join("") ||
        `<p>Nenhum cliente ativo no banco. Cadastre um cliente pelo <a href="/admin/clientes?new=1" data-api-link>painel administrativo</a>.</p>`
  }<p class="muted small">O cadastro público em /cadastro ainda segue o protótipo demonstrativo sem persistência.</p></div></div></main>${footer()}`;

const paginaDados = (c) =>
  layoutPerfil(
    "dados",
    "Meus dados",
    `<form id="perfil-dados-form" class="panel form-stack" data-testid="form-cliente"><div class="section-heading"><div><h2>${e(c.codigo)}</h2><p>${status(c.ativo)}</p></div><div><span class="muted small">Ranking de compras</span><h2>${c.ranking} / 5</h2></div></div>${camposPessoais(
      c,
      false,
    )}<div class="d-flex justify-content-between gap-2 flex-wrap"><button class="btn aura-btn" ${c.ativo ? "" : "disabled"}>Salvar alterações</button><div class="d-flex gap-2 flex-wrap"><button type="button" class="btn btn-outline-secondary" data-sair-cliente>Trocar de cliente</button><button type="button" class="btn btn-outline-danger" data-inativar-conta ${c.ativo ? "" : "disabled"}>Inativar minha conta</button></div></div></form>`,
  );

const paginaSeguranca = (c) =>
  layoutPerfil(
    "seguranca",
    "Segurança",
    `<form id="perfil-senha-form" class="panel form-stack" data-testid="form-senha"><h2>Alterar senha</h2><p class="muted small">A nova senha é gravada com BCrypt e precisa ter pelo menos 8 caracteres, letra maiúscula, minúscula e caractere especial.</p><label>Nova senha<input data-testid="nova-senha" class="form-control" type="password" name="novaSenha" required></label><label>Confirmar nova senha<input data-testid="confirmacao-senha" class="form-control" type="password" name="confirmacaoSenha" required></label><button class="btn aura-btn" ${c.ativo ? "" : "disabled"}>Atualizar senha</button></form>`,
  );

const finalidades = (x) =>
  [x.cobranca ? "Cobrança" : "", x.entrega ? "Entrega" : ""]
    .filter(Boolean)
    .join(" · ");

const paginaEnderecos = (c) => {
  const q = new URLSearchParams(location.search);
  const editando =
    c.enderecos.find((x) => String(x.id) === q.get("edit") && x.ativo) || null;
  const lista = c.enderecos
    .map(
      (x) =>
        `<article class="management-card ${x.ativo ? "" : "is-inactive"}"><div><h3>${e(x.apelido)} ${x.preferencialEntrega ? "<small>Preferencial</small>" : ""}</h3><p><b>${e(finalidades(x) || "Sem finalidade")}</b><br>${e(x.tipoLogradouro)} ${e(x.logradouro)}, ${e(x.numero)}${x.complemento ? " · " + e(x.complemento) : ""} · ${e(x.bairro)}<br>${e(x.cidade)}/${e(x.estado)} — ${e(x.pais)} · CEP ${e(x.cep)}${x.observacoes ? "<br>" + e(x.observacoes) : ""}</p></div><div class="management-actions">${x.ativo ? `<a href="/perfil/enderecos?edit=${x.id}" data-api-link>Editar</a>` : ""}${x.ativo && x.entrega && !x.preferencialEntrega ? `<button data-preferir-endereco="${x.id}">Tornar preferencial</button>` : ""}${x.ativo ? `<button data-inativar-endereco="${x.id}">Inativar</button>` : status(false)}</div></article>`,
    )
    .join("");
  return layoutPerfil(
    "enderecos",
    "Meus endereços",
    `<div class="panel"><div class="section-heading"><div><h2>Endereços cadastrados</h2><p class="muted">O cliente mantém ao menos um endereço de cobrança e um de entrega.</p></div></div>${lista || "<p>Nenhum endereço cadastrado.</p>"}</div><form id="perfil-endereco-form" class="panel form-stack mt-3" data-testid="form-endereco"><div class="section-heading"><div><h3>${editando ? "Editar endereço" : "Novo endereço"}</h3><p class="muted small">Todos os campos são obrigatórios, exceto complemento e observações.</p></div>${editando ? '<a href="/perfil/enderecos" data-api-link>Cancelar edição</a>' : ""}</div>${camposEndereco(
      editando || {},
      "endereco_",
    )}<div class="choice-grid"><label class="choice-card"><input type="checkbox" name="endereco_cobranca" ${editando ? (editando.cobranca ? "checked" : "") : "checked"}><span><b>Endereço de cobrança</b></span></label><label class="choice-card"><input type="checkbox" name="endereco_entrega" ${editando ? (editando.entrega ? "checked" : "") : "checked"}><span><b>Endereço de entrega</b></span></label></div><label class="form-check"><input class="form-check-input" type="checkbox" name="endereco_preferencialEntrega" ${editando?.preferencialEntrega ? "checked" : ""}> Tornar preferencial para entrega</label><button class="btn aura-btn" ${c.ativo ? "" : "disabled"}>${editando ? "Salvar alterações" : "Cadastrar endereço"}</button></form>`,
  );
};

const paginaCartoes = (c, bandeiras) => {
  const lista = c.cartoes
    .map(
      (x) =>
        `<article class="management-card ${x.ativo ? "" : "is-inactive"}"><div><h3>${e(x.bandeira)} final ${e(x.ultimosQuatro)} ${x.preferencial ? "<small>Preferencial</small>" : ""}</h3><p>Nome impresso: ${e(x.titular)}<br>O número completo e o código de segurança não são armazenados.</p></div><div class="management-actions">${x.ativo && !x.preferencial ? `<button data-preferir-cartao="${x.id}">Tornar preferencial</button>` : ""}${x.ativo ? `<button data-inativar-cartao="${x.id}">Inativar</button>` : status(false)}</div></article>`,
    )
    .join("");
  return layoutPerfil(
    "cartoes",
    "Meus cartões",
    `<div class="panel"><h2>Cartões cadastrados</h2>${lista || "<p>Nenhum cartão cadastrado.</p>"}</div><form id="perfil-cartao-form" class="panel form-stack mt-3" data-testid="form-cartao"><h3>Novo cartão</h3><p class="muted small">Use um número fictício válido. Os dados sensíveis são validados e descartados; só os quatro dígitos finais ficam persistidos.</p><div class="row g-3"><div class="col-md-6"><label>Número do cartão<input data-testid="numero" class="form-control" name="numero" inputmode="numeric" required></label></div><div class="col-md-6"><label>Nome impresso<input class="form-control" name="titular" value="${valor(
      c,
      "nome",
    )}" required></label></div><div class="col-md-6"><label>Bandeira<select class="form-select" name="bandeiraId" required>${bandeiras
      .map((b) => `<option value="${b.id}">${e(b.nome)}</option>`)
      .join("")}</select></label></div><div class="col-md-6"><label>Código de segurança<input data-testid="codigo-seguranca" class="form-control" name="codigoSeguranca" inputmode="numeric" maxlength="4" required></label></div></div><label class="form-check mt-3"><input class="form-check-input" type="checkbox" name="preferencial"> Definir como preferencial</label><button class="btn aura-btn" ${c.ativo ? "" : "disabled"}>Cadastrar cartão</button></form>`,
  );
};

function ligarEventos(raiz, navegar, toast, cliente) {
  const recarregar = () => renderizarPerfilApi(raiz, navegar, toast);

  raiz.querySelectorAll("[data-entrar-cliente]").forEach(
    (b) =>
      (b.onclick = () => {
        definirCliente(b.dataset.entrarCliente, b.dataset.nome);
        toast(`Sessão iniciada como ${b.dataset.nome}.`);
        recarregar();
      }),
  );

  if (!cliente) return;
  const id = cliente.id;

  raiz.querySelector("[data-sair-cliente]")?.addEventListener("click", () => {
    limparCliente();
    recarregar();
  });

  raiz
    .querySelector("#perfil-dados-form")
    ?.addEventListener("submit", (ev) => {
      ev.preventDefault();
      executar(
        ev.currentTarget,
        () => service.alterar(id, objetoForm(ev.currentTarget)),
        toast,
        recarregar,
      );
    });

  raiz.querySelector("[data-inativar-conta]")?.addEventListener("click", () => {
    if (
      !confirm(
        "Inativar a conta? O histórico é preservado, mas novas compras ficam bloqueadas.",
      )
    )
      return;
    executar(
      raiz.querySelector("#perfil-dados-form"),
      () => service.inativar(id),
      toast,
      recarregar,
    );
  });

  raiz
    .querySelector("#perfil-senha-form")
    ?.addEventListener("submit", (ev) => {
      ev.preventDefault();
      executar(
        ev.currentTarget,
        () => service.alterarSenha(id, objetoForm(ev.currentTarget)),
        toast,
        recarregar,
      );
    });

  const formEndereco = raiz.querySelector("#perfil-endereco-form");
  formEndereco?.addEventListener("submit", (ev) => {
    ev.preventDefault();
    const q = new URLSearchParams(location.search);
    const editando = q.get("edit");
    const payload = enderecoDe(objetoForm(ev.currentTarget), "endereco_");
    executar(
      ev.currentTarget,
      () =>
        editando
          ? service.alterarEndereco(id, editando, payload)
          : service.adicionarEndereco(id, payload),
      toast,
      () => navegar("/perfil/enderecos"),
    );
  });
  raiz.querySelectorAll("[data-preferir-endereco]").forEach(
    (b) =>
      (b.onclick = () => {
        const x = cliente.enderecos.find(
          (item) => String(item.id) === b.dataset.preferirEndereco,
        );
        executar(
          formEndereco || raiz.querySelector("form"),
          () =>
            service.alterarEndereco(id, x.id, {
              apelido: x.apelido,
              tipoResidencia: x.tipoResidencia,
              tipoLogradouro: x.tipoLogradouro,
              logradouro: x.logradouro,
              numero: x.numero,
              bairro: x.bairro,
              cep: x.cep,
              cidade: x.cidade,
              estado: x.estado,
              pais: x.pais,
              observacoes: x.observacoes || "",
              complemento: x.complemento || "",
              cobranca: x.cobranca,
              entrega: x.entrega,
              preferencialEntrega: true,
            }),
          toast,
          recarregar,
        );
      }),
  );
  raiz.querySelectorAll("[data-inativar-endereco]").forEach(
    (b) =>
      (b.onclick = () => {
        if (!confirm("Inativar este endereço?")) return;
        executar(
          formEndereco || raiz.querySelector("form"),
          () => service.inativarEndereco(id, b.dataset.inativarEndereco),
          toast,
          recarregar,
        );
      }),
  );

  raiz
    .querySelector("#perfil-cartao-form")
    ?.addEventListener("submit", (ev) => {
      ev.preventDefault();
      const d = objetoForm(ev.currentTarget);
      executar(
        ev.currentTarget,
        () =>
          service.adicionarCartao(id, {
            numero: d.numero,
            titular: d.titular,
            bandeiraId: Number(d.bandeiraId),
            codigoSeguranca: d.codigoSeguranca,
            preferencial: d.preferencial === "on",
          }),
        toast,
        recarregar,
      );
    });
  raiz.querySelectorAll("[data-preferir-cartao]").forEach(
    (b) =>
      (b.onclick = () =>
        executar(
          raiz.querySelector("#perfil-cartao-form"),
          () => service.preferirCartao(id, b.dataset.preferirCartao),
          toast,
          recarregar,
        )),
  );
  raiz.querySelectorAll("[data-inativar-cartao]").forEach(
    (b) =>
      (b.onclick = () => {
        if (!confirm("Inativar este cartão?")) return;
        executar(
          raiz.querySelector("#perfil-cartao-form"),
          () => service.inativarCartao(id, b.dataset.inativarCartao),
          toast,
          recarregar,
        );
      }),
  );
}

function ligarEventosEscolha(navegar) {
  ligarLinks(navegar);
}

export async function renderizarPerfilApi(raiz, navegar, toast) {
  const versao = ++renderVersion,
    rota = location.href;
  const id = lerClienteId();
  let cliente = null,
    bandeiras = [],
    cadastrados = [],
    falha = null;

  if (id) {
    try {
      cliente = await service.buscar(id);
      definirCliente(id, cliente.nome);
    } catch {
      limparCliente();
    }
  }
  try {
    if (cliente && location.pathname === "/perfil/cartoes")
      bandeiras = await service.bandeiras();
    if (!cliente) cadastrados = await service.listar({ ativo: true });
  } catch (err) {
    falha = err.message;
  }
  if (rota !== location.href || versao !== renderVersion) return;

  if (!cliente) {
    raiz.innerHTML = paginaEscolha(cadastrados, falha);
    prepararFormularios(raiz);
    ligarEventosEscolha(navegar);
    ligarEventos(raiz, navegar, toast, null);
    return;
  }

  const caminho = location.pathname;
  const html =
    caminho === "/perfil/seguranca"
      ? paginaSeguranca(cliente)
      : caminho === "/perfil/enderecos"
        ? paginaEnderecos(cliente)
        : caminho === "/perfil/cartoes"
          ? paginaCartoes(cliente, bandeiras)
          : paginaDados(cliente);
  raiz.innerHTML = html;
  prepararFormularios(raiz);
  ligarLinks(navegar);
  ligarEventos(raiz, navegar, toast, cliente);
}
