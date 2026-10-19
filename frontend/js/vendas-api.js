/**
 * Loja integrada à API real de vendas: catálogo, carrinho e checkout persistidos no
 * PostgreSQL (RF0031–RF0038, RN0031–RN0036). Segue o mesmo padrão de perfil-api.js: um
 * cliente real é escolhido para a sessão (localStorage) e todas as ações usam os services
 * reais. A jornada integrada ocupa as rotas públicas canônicas da loja; não existe um segundo
 * frontend de vendas em paralelo.
 */
import { produtosService } from "./produtosService.js";
import { clientesService } from "./clientesService.js";
import { vendasService } from "./vendasService.js";
import { escaparHtml as e, formatarMoeda as moeda } from "./utilitarios.js";
import { camposEndereco } from "./clientesComponentes.js";
import { prepararFormularios } from "./clientesEventos.js";
import { header, footer } from "./pages.js";

const CHAVE_CLIENTE = "aura-lab-perfil-api-cliente";
const CHAVE_NOME = "aura-lab-perfil-api-nome";

export const ehRotaVendasApi = () =>
  [
    "/",
    "/catalogo",
    "/sacola",
    "/checkout",
    "/checkout/endereco",
    "/checkout/pagamento",
    "/checkout/revisao",
    "/checkout/confirmacao",
    "/perfil/cupons",
  ].includes(location.pathname) ||
  location.pathname.startsWith("/produto/") ||
  location.pathname.startsWith("/checkout/confirmacao/") ||
  location.pathname.startsWith("/loja");

let versao = 0;
let consultaCarrinho;
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
const enderecoDe = (d, p = "endereco_") => ({
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
  cobranca: false,
  entrega: true,
  preferencialEntrega: false,
});

const cabecalho = (nome, totalItens = 0) =>
  `<header class="site-header"><div class="notice">Beleza que acompanha o seu ritmo <span>•</span></div><nav class="navbar navbar-expand-lg container aura-container"><a href="/" data-loja-link class="brand">aura <i>LAB</i></a><button class="navbar-toggler border-0" data-bs-toggle="collapse" data-bs-target="#nav" aria-label="Abrir menu">☰</button><div id="nav" class="collapse navbar-collapse"><ul class="navbar-nav mx-auto nav-main"><li><a href="/" data-loja-link class="${["/", "/catalogo"].includes(location.pathname) ? "active" : ""}">Produtos</a></li><li><a href="/consultora" data-loja-link>Consultora Aura</a></li><li><a href="/perfil/cupons" data-loja-link class="${location.pathname === "/perfil/cupons" ? "active" : ""}">Meus cupons</a></li></ul><div class="nav-actions"><a href="/perfil/dados" data-loja-link><svg class="profile-icon" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3.5"/><path d="M4.5 20c.8-4 3.3-6 7.5-6s6.7 2 7.5 6"/></svg><span>${e(nome?.split(" ")[0] || "Escolher cliente")}</span></a><a href="/sacola" data-loja-link data-testid="ir-carrinho"><svg class="profile-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 7a3 3 0 016 0v1"/><path d="M6 8h12l-1 13H7L6 8z"/></svg><span>Sacola</span><b>${totalItens}</b></a></div></div></nav></header>`;

const layout = (nome, titulo, conteudo, totalItens = 0, eyebrow = "Minha Aura") =>
  `${cabecalho(nome, totalItens)}<main class="page"><div class="container aura-container page-top"><span class="eyebrow">${e(eyebrow)}</span><h1>${e(titulo)}</h1>${conteudo}</div></main>${footer()}`;

const paginaEscolha = (clientes, falha) =>
  `${header()}<main class="page"><div class="container aura-container page-top"><span class="eyebrow">Área do cliente</span><h1>Selecione um cliente cadastrado</h1><div class="panel form-stack">${
    falha
      ? `<div class="alert alert-danger">${e(falha)}</div>`
      : clientes
          .map(
            (c) =>
              `<button class="btn aura-btn" data-entrar-cliente="${c.id}" data-nome="${e(
                c.nome,
              )}">Entrar como ${e(c.nome)} · ${e(c.codigo)}</button>`,
          )
          .join("") ||
        `<p>Nenhum cliente ativo. Cadastre um pelo <a href="/admin/clientes?new=1" data-loja-link>painel administrativo</a>.</p>`
  }</div></div></main>${footer()}`;

// ---- catálogo ----
const paginaCatalogo = (nome, produtos, totalItens) =>
  `${cabecalho(nome, totalItens)}<main class="page"><div class="container aura-container catalog-content"><div class="row g-4"><aside class="col-lg-3"><div class="panel filters"><div class="d-flex justify-content-between"><b>Filtros</b><a href="/" data-loja-link>Limpar</a></div><hr><label><input type="radio" checked> Todos</label><label class="mt-3">Marca<select class="form-select mt-1" data-catalogo-marca><option value="">Todas</option>${[...new Set(produtos.map((p) => p.marca))].map((marca) => `<option>${e(marca)}</option>`).join("")}</select></label></div></aside><section class="col-lg-9"><div class="catalog-search"><label for="search">Encontre seu próximo ritual</label><div class="input-group"><span class="input-group-text">⌕</span><input id="search" class="form-control" data-catalogo-busca placeholder="Buscar por produto ou marca"></div></div><div class="catalog-heading"><p class="muted"><span data-catalogo-total>${produtos.length}</span> produtos encontrados</p><span>Ordenar por <b>Mais relevantes</b></span></div><div class="row g-3" data-catalogo-grid>${
    produtos.length
      ? produtos
          .map(
            (p, indice) =>
              `<div class="col-sm-6 col-xl-4" data-catalogo-produto data-nome="${e(p.nome.toLowerCase())}" data-marca="${e(p.marca)}"><article class="aura-card" data-testid="produto-${p.id}"><div class="product-art" data-cor-produto="${["#d7a3ae", "#b7c9bc", "#cab7d9", "#e1c29d"][indice % 4]}"><span class="product-mark">aura</span></div><div class="card-body"><p class="product-brand">${e(p.marca)}</p><a class="product-link" href="/produto/${p.id}" data-loja-link>${e(p.nome)}</a><p class="availability">${p.estoqueDisponivel < 8 ? "Poucas unidades" : "Disponível"}</p><div class="d-flex align-items-center gap-2"><b>${moeda(p.preco)}</b><input class="form-control ms-auto" style="max-width:70px" type="number" min="1" max="${p.estoqueDisponivel}" value="1" data-testid="qtd-${p.id}" aria-label="Quantidade de ${e(p.nome)}"><button class="round" data-add="${p.id}" data-testid="add-${p.id}" aria-label="Adicionar ${e(p.nome)}">＋</button></div></div></article></div>`,
          )
          .join("")
      : '<div class="panel">Nenhum produto disponível no momento.</div>'
  }</div></section></div></div></main>${footer()}`;

const paginaProduto = (nome, p, totalItens) =>
  `${cabecalho(nome, totalItens)}<main class="page"><div class="container aura-container"><a href="/" data-loja-link class="back">← Produtos</a><div class="row g-5 mt-1 align-items-center"><div class="col-lg-6"><div class="detail-art" data-cor-produto="#d7a3ae"><span class="product-mark">aura</span></div></div><div class="col-lg-6"><small>${e(p.marca)}</small><h1>${e(p.nome)}</h1><h3>${moeda(p.preco)}</h3><p>Código ${e(p.codigo)} · ${p.estoqueDisponivel} unidade(s) disponíveis.</p><div class="chips"><span>${p.estoqueDisponivel > 0 ? "Disponível" : "Esgotado"}</span></div><div class="product-buy"><label for="product-quantity">Quantidade</label><input id="product-quantity" class="form-control" type="number" min="1" max="${p.estoqueDisponivel}" value="1" data-testid="qtd-${p.id}"><button class="btn aura-btn" data-add="${p.id}" data-testid="add-${p.id}" ${p.estoqueDisponivel > 0 ? "" : "disabled"}>Adicionar à sacola</button></div></div></div></div></main>${footer()}`;

// ---- carrinho ----
const paginaCarrinho = (nome, carrinho) => {
  const itens = carrinho.itens || [];
  const linhas = itens
    .map(
      (i) =>
        `<div class="cart-row" data-testid="item-${i.id}"><div><small>${e(
          i.codigo,
        )}</small><b>${e(
          i.nome,
        )}</b><span class="muted">${moeda(
          i.precoUnitario,
        )} por unidade</span>${
          i.disponivel
            ? ""
            : '<p class="text-danger small" data-testid="indisponivel">Quantidade indisponível em estoque.</p>'
        }</div><div class="d-flex gap-2 align-items-center"><input class="form-control" style="width:80px" type="number" min="1" max="${i.estoqueDisponivel}" value="${i.quantidade}" data-item-carrinho="${i.id}" data-testid="qtd-item-${i.id}" aria-label="Quantidade"></div><b>${moeda(
          i.subtotal,
        )}</b><button class="link-danger" data-remover="${i.id}" data-testid="remover-${i.id}">Remover</button></div>`,
    )
    .join("");
  return layout(
    nome,
    "Seu ritual está quase pronto.",
    `${carrinho.venceEm ? `<div class="alert-soft" data-testid="prazo-reserva">Reserva até ${e(new Date(carrinho.venceEm).toLocaleString("pt-BR"))}. Prazo: ${carrinho.prazoMinutos} minutos após a última inclusão.</div>` : ""}${(carrinho.avisos || []).map(n => `<p class="alert-soft" data-testid="aviso-reserva">${e(n.mensagem)}</p>`).join("")}${(carrinho.retirados || []).filter(r => !r.readicionado).map(r => `<div class="alert alert-warning" data-testid="item-retirado">${e(r.produtoNome)} × ${r.quantidade} · ${e(r.motivo)} · prazo ${r.prazoMinutos} minutos. Faça uma nova inclusão para reservar novamente.</div>`).join("")}${
      itens.length
        ? `<div class="row g-4"><section class="col-lg-8 panel">${linhas}</section><aside class="col-lg-4"><div class="panel summary"><h3>Resumo</h3><p>Produtos <b data-testid="carrinho-subtotal">${moeda(
            carrinho.subtotal,
          )}</b></p><p>Frete <b>calculado pelo endereço</b></p><a class="btn aura-btn w-100" href="/checkout/endereco" data-loja-link data-testid="ir-checkout">Iniciar checkout</a></div></aside></div>`
        : '<div class="panel text-center" data-testid="carrinho-vazio">Sua sacola está vazia. <a href="/" data-loja-link>Explorar produtos</a></div>'
    }`,
    carrinho.totalUnidades,
    "Sua sacola",
  );
};

// ---- checkout ----
const linhaResumo = (r) =>
  `<ul class="list-unstyled"><li>Subtotal: <b data-testid="resumo-subtotal">${moeda(
    r.subtotal,
  )}</b></li><li>Desconto: <b data-testid="resumo-desconto">${moeda(
    r.desconto,
  )}</b></li><li>Frete: <b data-testid="resumo-frete">${moeda(
    r.frete,
  )}</b></li><li>Total: <b data-testid="resumo-total">${moeda(r.total)}</b></li></ul>`;

const paginaCheckout = (
  nome,
  cliente,
  cupons,
  bandeiras,
  resumo,
  enderecoPadrao,
  totalItens,
) => {
  const enderecos = cliente.enderecos.filter((x) => x.ativo && x.entrega);
  const cartoes = cliente.cartoes.filter((x) => x.ativo);
  const opcoesEndereco = enderecos
    .map(
      (x) =>
        `<label class="choice-card"><input type="radio" name="enderecoId" value="${x.id}" data-testid="endereco-${x.id}" ${
          x.id === enderecoPadrao ? "checked" : ""
        }><span><b>${e(x.apelido || "Endereço")}</b><br>${e(x.logradouro)}, ${e(
          x.numero,
        )} · ${e(x.cidade)}/${e(x.estado)}</span></label>`,
    )
    .join("");
  const opcoesCupom = cupons.length
    ? cupons
        .map(
          (c) =>
            `<label class="choice-card coupon-choice"><input type="checkbox" name="cupomTroca" value="${c.id}" data-testid="cupom-${c.id}"><span><b>${e(
              c.codigo,
            )}</b><small>Crédito disponível: ${moeda(c.valor)}</small></span></label>`,
        )
        .join("")
    : '<p class="muted small">Nenhum cupom de troca disponível.</p>';
  const linhasCartoes = cartoes.length
    ? cartoes
        .map(
          (x) =>
            `<label class="payment-card"><span><b>${e(x.bandeira)} final ${e(
              x.ultimosQuatro,
            )}</b><small>${e(x.titular)}${x.preferencial ? " · Preferencial" : ""}</small></span><input class="form-control" type="number" step="0.01" min="0" value="0" name="valorCartao_${x.id}" data-testid="valor-cartao-${x.id}" aria-label="Valor no cartão ${e(x.bandeira)} final ${e(x.ultimosQuatro)}" placeholder="R$ 0,00"></label>`,
        )
        .join("")
    : '<p class="muted small">Nenhum cartão salvo. Use um novo cartão abaixo.</p>';
  return layout(
    nome,
    "Finalize sua compra com tranquilidade.",
    `<ol class="checkout-steps"><li class="active"><span>1</span>Entrega</li><li class="active"><span>2</span>Pagamento</li><li><span>3</span>Confirmação</li></ol><div class="row g-4"><section class="col-lg-8"><form id="loja-checkout-form" data-testid="form-checkout" class="panel checkout-form form-stack">
      <h2>Endereço de entrega</h2>
      <p class="muted small">Escolha um endereço salvo ou informe um novo para esta compra.</p>
      <div class="choice-grid">${opcoesEndereco}<label class="choice-card"><input type="radio" name="enderecoId" value="novo" data-testid="endereco-novo" ${
        enderecos.length ? "" : "checked"
      }><span><b>Usar novo endereço</b><small>Preencha os dados obrigatórios abaixo.</small></span></label></div>
      <div class="new-address-fields ${enderecos.length ? "hidden-section" : ""}" data-novo-endereco><h3>Novo endereço</h3>${camposEndereco(
        {},
        "endereco_",
      )}<label class="form-check"><input class="form-check-input" type="checkbox" name="incorporarEndereco" data-testid="incorporar-endereco"> Salvar este endereço no meu perfil</label></div>
      <hr>
      <h2>Pagamento</h2>
      <label>Cupom promocional <small>(opcional)</small><div class="input-group"><input class="form-control" name="cupomPromocional" data-testid="cupom-promocional" placeholder="Ex.: AURA10"></div></label>
      <p class="muted small">Use no máximo um cupom promocional. Cupons de troca entram como crédito no pagamento.</p>
      <div><h3>Cupons de troca</h3><div class="choice-grid">${opcoesCupom}</div></div>
      <div class="section-heading"><div><h3>Cartões de crédito</h3><p class="muted small">Informe o valor que será cobrado em cada cartão.</p></div></div>${linhasCartoes}
      <details class="checkout-new-card"><summary data-testid="novo-cartao">Adicionar novo cartão</summary><div class="row g-3 mt-2"><div class="col-md-6"><label>Número<input class="form-control" name="cartao_numero" inputmode="numeric" data-testid="cartao-numero"></label></div><div class="col-md-6"><label>Nome impresso<input class="form-control" name="cartao_titular" data-testid="cartao-titular"></label></div><div class="col-md-4"><label>Bandeira<select class="form-select" name="cartao_bandeiraId" data-testid="cartao-bandeira">${bandeiras
        .map((b) => `<option value="${b.id}">${e(b.nome)}</option>`)
        .join(
          "",
        )}</select></label></div><div class="col-md-4"><label>CVV<input class="form-control" name="cartao_codigoSeguranca" inputmode="numeric" maxlength="4" data-testid="cartao-cvv"></label></div><div class="col-md-4"><label>Valor neste cartão<input class="form-control" type="number" step="0.01" min="0" value="0" name="valorCartaoNovo" data-testid="valor-cartao-novo"></label></div><div class="col-12"><label class="form-check"><input class="form-check-input" type="checkbox" name="incorporarCartao" data-testid="incorporar-cartao"> Salvar este cartão no meu perfil</label></div></div></details>
      <div class="checkout-actions">
      <button type="submit" class="btn aura-btn" data-testid="finalizar-compra">Finalizar compra</button>
      </div></form></section><aside class="col-lg-4"><div class="panel summary payment-summary"><h3>Resumo da compra</h3><p data-testid="estado-resumo" role="status" aria-live="polite"></p><div data-testid="resumo-lateral">${linhaResumo(resumo)}</div><p class="muted small">O pedido será registrado com status <b>EM PROCESSAMENTO</b>.</p></div></aside></div>`,
    totalItens,
    "Checkout",
  );
};

// ---- confirmação ----
const paginaConfirmacao = (nome, pedido) =>
  layout(
    nome,
    "Obrigada pela sua compra.",
    `<div class="panel confirmation" data-testid="pedido-confirmado"><span class="confirmation-mark">✓</span><div class="confirmation-heading"><span class="eyebrow">Pedido ${e(
      pedido.codigo,
    )}</span><h2>Status: <span data-testid="pedido-status">${e(
      pedido.status.replace("_", " "),
    )}</span></h2></div><ul class="list-unstyled"><li>Subtotal: ${moeda(
      pedido.subtotal,
    )}</li><li>Desconto: ${moeda(pedido.desconto)}</li><li>Frete: ${moeda(
      pedido.frete,
    )}</li><li><b>Total: ${moeda(pedido.total)}</b></li></ul><h3 class="h6">Itens</h3><ul>${pedido.itens
      .map(
        (i) =>
          `<li>${i.quantidade}× ${e(i.nome)} — ${moeda(i.subtotal)}</li>`,
      )
      .join("")}</ul><h3 class="h6">Pagamento</h3><ul>${pedido.pagamentos
      .map((p) => `<li>${e(p.descricao)} — ${moeda(p.valor)}</li>`)
      .join("")}</ul>${
      pedido.cupomTrocaGerado
        ? `<div class="alert alert-info" data-testid="cupom-troco">Cupom de troco gerado: ${e(
            pedido.cupomTrocaGerado.codigo,
          )} — ${moeda(pedido.cupomTrocaGerado.valor)}</div>`
        : ""
    }<div class="d-flex gap-2 justify-content-center flex-wrap"><a class="btn aura-btn" href="/perfil/cupons" data-loja-link>Ver meus cupons</a><a class="btn btn-outline-secondary" href="/" data-loja-link>Continuar comprando</a></div></div>`,
    0,
    "Pedido recebido",
  );

// ---- cupons ----
const paginaCupons = (nome, cupons) =>
  layout(
    nome,
    "Meus cupons",
    `<div class="panel">${
      cupons.length
        ? cupons
            .map(
              (c) =>
                `<article class="management-card ${
                  c.situacao === "ATIVO" ? "" : "is-inactive"
                }" data-testid="cupom-linha-${c.id}"><div><h3>${e(
                  c.codigo,
                )}</h3><p class="muted small">${e(c.origem || "")}</p></div><div><b>${moeda(
                  c.valor,
                )}</b> · ${e(c.situacao)}</div></article>`,
            )
            .join("")
        : "<p>Você ainda não tem cupons de troca.</p>"
    }</div>`,
    0,
    "Cliente cadastrado",
  );

// ---- eventos ----
function ligarLinks(navegar) {
  document.querySelectorAll("[data-loja-link]").forEach(
    (a) =>
      (a.onclick = (ev) => {
        ev.preventDefault();
        navegar(a.getAttribute("href"));
      }),
  );
  document.querySelector("[data-sair-cliente]")?.addEventListener("click", (ev) => {
    ev.preventDefault();
    limparCliente();
    navegar("/");
  });
}

function toNum(v) {
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
}

async function acao(botao, fn, toast, apos) {
  if (botao?.disabled) return;
  if (botao) botao.disabled = true;
  try {
    await fn();
    if (apos) await apos();
  } catch (err) {
    toast(err.message, "error");
    if (botao) botao.disabled = false;
  }
}

function ligarFiltroCatalogo(raiz) {
  const busca = raiz.querySelector("[data-catalogo-busca]");
  const marca = raiz.querySelector("[data-catalogo-marca]");
  const aplicar = () => {
    const termo = (busca?.value || "").trim().toLowerCase();
    const marcaSelecionada = marca?.value || "";
    let visiveis = 0;
    raiz.querySelectorAll("[data-catalogo-produto]").forEach((item) => {
      const mostrar =
        (!termo || item.dataset.nome.includes(termo)) &&
        (!marcaSelecionada || item.dataset.marca === marcaSelecionada);
      item.classList.toggle("d-none", !mostrar);
      if (mostrar) visiveis += 1;
    });
    const total = raiz.querySelector("[data-catalogo-total]");
    if (total) total.textContent = String(visiveis);
  };
  busca?.addEventListener("input", aplicar);
  marca?.addEventListener("change", aplicar);
}

function ligarAdicao(raiz, navegar, toast, clienteId) {
  raiz.querySelectorAll("[data-add]").forEach(
    (botao) =>
      (botao.onclick = () => {
        const produtoId = Number(botao.dataset.add);
        const quantidade = toNum(
          raiz.querySelector(`[data-testid='qtd-${produtoId}']`)?.value || 1,
        );
        acao(
          botao,
          () =>
            vendasService.adicionarItem(clienteId, {
              produtoId,
              quantidade,
            }),
          toast,
          () => {
            toast(`${quantidade} item(ns) adicionado(s) à sacola.`);
            renderizarVendasApi(raiz, navegar, toast);
          },
        );
      }),
  );
}

export async function renderizarVendasApi(raiz, navegar, toast) {
  clearTimeout(consultaCarrinho);
  const v = ++versao;
  const rota = location.href;
  const id = lerClienteId();
  const caminho = location.pathname;

  const aliases = {
    "/loja": "/",
    "/loja/carrinho": "/sacola",
    "/loja/checkout": "/checkout/endereco",
    "/loja/cupons": "/perfil/cupons",
    "/checkout/pagamento": "/checkout/endereco",
    "/checkout/revisao": "/checkout/endereco",
    "/checkout/confirmacao": "/sacola",
  };
  if (aliases[caminho]) {
    navegar(aliases[caminho]);
    return;
  }
  if (caminho.startsWith("/loja/confirmacao/")) {
    navegar(caminho.replace("/loja/confirmacao/", "/checkout/confirmacao/"));
    return;
  }

  if (!id) {
    let clientes = [],
      falha = null;
    try {
      clientes = await clientesService.listar({ ativo: true });
    } catch (err) {
      falha = err.message;
    }
    if (rota !== location.href || v !== versao) return;
    raiz.innerHTML = paginaEscolha(clientes, falha);
    raiz.querySelectorAll("[data-entrar-cliente]").forEach(
      (b) =>
        (b.onclick = () => {
          definirCliente(b.dataset.entrarCliente, b.dataset.nome);
          navegar("/");
        }),
    );
    ligarLinks(navegar);
    return;
  }

  let cliente;
  try {
    cliente = await clientesService.buscar(id);
    definirCliente(id, cliente.nome);
  } catch {
    limparCliente();
    return renderizarVendasApi(raiz, navegar, toast);
  }
  const nome = cliente.nome;

  try {
    if (caminho === "/" || caminho === "/catalogo") {
      const [produtos, carrinho] = await Promise.all([
        produtosService.listar({ ativo: true }),
        vendasService.carrinho(id),
      ]);
      if (rota !== location.href || v !== versao) return;
      const disponiveis = produtos.filter((p) => p.visivel && p.estoqueDisponivel > 0);
      raiz.innerHTML = paginaCatalogo(nome, disponiveis, carrinho.totalUnidades);
      ligarLinks(navegar);
      ligarFiltroCatalogo(raiz);
      ligarAdicao(raiz, navegar, toast, id);
    } else if (caminho.startsWith("/produto/")) {
      const produtoId = Number(caminho.split("/").filter(Boolean)[1]);
      const [produto, carrinho] = await Promise.all([
        produtosService.buscar(produtoId),
        vendasService.carrinho(id),
      ]);
      if (rota !== location.href || v !== versao) return;
      raiz.innerHTML = paginaProduto(nome, produto, carrinho.totalUnidades);
      ligarLinks(navegar);
      ligarAdicao(raiz, navegar, toast, id);
    } else if (caminho === "/sacola") {
      const carrinho = await vendasService.carrinho(id);
      if (rota !== location.href || v !== versao) return;
      raiz.innerHTML = paginaCarrinho(nome, carrinho);
      const assinatura = JSON.stringify(carrinho);
      const sincronizar = async () => {
        if (location.href !== rota || v !== versao) return;
        try {
          const novo = await vendasService.carrinho(id);
          if (location.href !== rota || v !== versao) return;
          if (JSON.stringify(novo) !== assinatura) { renderizarVendasApi(raiz, navegar, toast); return; }
        } catch (err) { toast(err.message, "error"); }
        consultaCarrinho = setTimeout(sincronizar, 5000);
      };
      consultaCarrinho = setTimeout(sincronizar, 5000);
      ligarLinks(navegar);
      const temporizadoresQuantidade = new Map();
      raiz.querySelectorAll("[data-item-carrinho]").forEach((campo) => {
        campo.addEventListener("input", () => {
          const itemId = campo.dataset.itemCarrinho;
          clearTimeout(temporizadoresQuantidade.get(itemId));
          temporizadoresQuantidade.set(
            itemId,
            setTimeout(async () => {
              if (!campo.isConnected || campo.disabled) return;
              campo.disabled = true;
              try {
                await vendasService.alterarItem(id, itemId, { quantidade: toNum(campo.value) });
                toast("Quantidade atualizada.");
                renderizarVendasApi(raiz, navegar, toast);
              } catch (err) {
                toast(err.message, "error");
                campo.disabled = false;
              }
            }, 350),
          );
        });
      });
      raiz.querySelectorAll("[data-remover]").forEach(
        (b) =>
          (b.onclick = () =>
            acao(
              b,
              () => vendasService.removerItem(id, b.dataset.remover),
              toast,
              () => {
                toast("Item removido.");
                renderizarVendasApi(raiz, navegar, toast);
              },
            )),
      );
    } else if (["/checkout", "/checkout/endereco"].includes(caminho)) {
      const [carrinho, cupons, bandeiras] = await Promise.all([
        vendasService.carrinho(id),
        vendasService.cupons(id),
        clientesService.bandeiras(),
      ]);
      if (rota !== location.href || v !== versao) return;
      if (!carrinho.itens || !carrinho.itens.length) {
        raiz.innerHTML = paginaCarrinho(nome, carrinho);
      const assinatura = JSON.stringify(carrinho);
      const sincronizar = async () => {
        if (location.href !== rota || v !== versao) return;
        try {
          const novo = await vendasService.carrinho(id);
          if (location.href !== rota || v !== versao) return;
          if (JSON.stringify(novo) !== assinatura) { renderizarVendasApi(raiz, navegar, toast); return; }
        } catch (err) { toast(err.message, "error"); }
        consultaCarrinho = setTimeout(sincronizar, 5000);
      };
      consultaCarrinho = setTimeout(sincronizar, 5000);
        ligarLinks(navegar);
        return;
      }
      const enderecos = cliente.enderecos.filter((x) => x.ativo && x.entrega);
      const cuponsAtivos = cupons.filter((c) => c.situacao === "ATIVO");
      const padrao =
        enderecos.find((x) => x.preferencialEntrega)?.id || enderecos[0]?.id || null;
      let resumo = { subtotal: carrinho.subtotal, desconto: 0, frete: 0, total: carrinho.subtotal };
      try {
        resumo = await vendasService.resumo(id, padrao ? { enderecoId: padrao } : {});
      } catch {}
      if (rota !== location.href || v !== versao) return;
      raiz.innerHTML = paginaCheckout(
        nome,
        cliente,
        cuponsAtivos,
        bandeiras,
        resumo,
        padrao,
        carrinho.totalUnidades,
      );
      prepararFormularios(raiz);
      ligarLinks(navegar);
      ligarCheckout(raiz, navegar, toast, id, cliente);
    } else if (caminho.startsWith("/checkout/confirmacao/")) {
      const pedidoId = decodeURIComponent(caminho.split("/").filter(Boolean)[2]);
      const pedido = await vendasService.pedido(id, pedidoId);
      if (rota !== location.href || v !== versao) return;
      raiz.innerHTML = paginaConfirmacao(nome, pedido);
      ligarLinks(navegar);
    } else if (caminho === "/perfil/cupons") {
      const cupons = await vendasService.cupons(id);
      if (rota !== location.href || v !== versao) return;
      raiz.innerHTML = paginaCupons(nome, cupons);
      ligarLinks(navegar);
    } else {
      navegar("/");
    }
  } catch (err) {
    if (rota !== location.href || v !== versao) return;
    raiz.innerHTML = layout(nome, "Loja", `<div class="alert alert-danger">${e(err.message)}</div>`);
    ligarLinks(navegar);
  }
}

function montarPayload(form, cliente) {
  const d = objetoForm(form);
  const payload = {};
  if (d.enderecoId === "novo") {
    payload.novoEndereco = enderecoDe(d, "endereco_");
    payload.incorporarEndereco = d.incorporarEndereco === "on";
  } else {
    payload.enderecoId = Number(d.enderecoId);
  }
  const promo = (d.cupomPromocional || "").trim();
  if (promo) payload.cupomPromocional = promo;
  payload.cuponsTroca = new FormData(form).getAll("cupomTroca").map(Number);
  const cartoes = [];
  cliente.cartoes
    .filter((x) => x.ativo)
    .forEach((x) => {
      const valor = toNum(d["valorCartao_" + x.id]);
      if (valor > 0) cartoes.push({ cartaoId: x.id, valor });
    });
  const valorNovo = toNum(d.valorCartaoNovo);
  if (valorNovo > 0)
    cartoes.push({
      novoCartao: {
        numero: d.cartao_numero,
        titular: d.cartao_titular,
        bandeiraId: Number(d.cartao_bandeiraId),
        codigoSeguranca: d.cartao_codigoSeguranca,
        preferencial: false,
      },
      incorporarCartao: d.incorporarCartao === "on",
      valor: valorNovo,
    });
  payload.cartoes = cartoes;
  return payload;
}

function estadoSelecionado(form, cliente) {
  const d = objetoForm(form);
  if (d.enderecoId === "novo") return { estado: d.endereco_estado || "" };
  return { enderecoId: Number(d.enderecoId) };
}

function ligarCheckout(raiz, navegar, toast, id, cliente) {
  const form = raiz.querySelector("#loja-checkout-form");
  const promoInput = form.querySelector("[name='cupomPromocional']");
  const camposNovoEndereco = form.querySelector("[data-novo-endereco]");
  const atualizarNovoEndereco = () => {
    const novo = form.querySelector("[name='enderecoId']:checked")?.value === "novo";
    camposNovoEndereco?.classList.toggle("hidden-section", !novo);
  };
  form.querySelectorAll("[name='enderecoId']").forEach((campo) =>
    campo.addEventListener("change", atualizarNovoEndereco),
  );
  atualizarNovoEndereco();

  const painelResumo = raiz.querySelector("[data-testid='resumo-lateral']");
  const estadoResumo = raiz.querySelector("[data-testid='estado-resumo']");
  let revisaoResumo = 0;
  let temporizadorResumo;
  const agendarResumo = () => {
    clearTimeout(temporizadorResumo);
    const revisao = ++revisaoResumo;
    estadoResumo.textContent = "Atualizando resumo…";
    estadoResumo.dataset.situacao = "calculando";
    painelResumo.setAttribute("aria-busy", "true");
    form.querySelector("[data-testid='finalizar-compra']").disabled = true;
    temporizadorResumo = setTimeout(async () => {
      if (!form.isConnected) return;
      const base = estadoSelecionado(form, cliente);
      const promo = (promoInput.value || "").trim();
      try {
        const r = await vendasService.resumo(
          id, promo ? { ...base, cupomPromocional: promo } : base,
        );
        if (!form.isConnected || revisao !== revisaoResumo) return;
        painelResumo.innerHTML = linhaResumo(r);
        estadoResumo.textContent = "Resumo atualizado automaticamente.";
        estadoResumo.dataset.situacao = "pronto";
      } catch (err) {
        if (!form.isConnected || revisao !== revisaoResumo) return;
        estadoResumo.textContent = err.message;
        estadoResumo.dataset.situacao = "erro";
        toast(err.message, "error");
      } finally {
        if (form.isConnected && revisao === revisaoResumo) {
          painelResumo.setAttribute("aria-busy", "false");
          if (!form.dataset.enviando)
            form.querySelector("[data-testid='finalizar-compra']").disabled = false;
        }
      }
    }, 350);
  };
  const aoAlterarResumo = (ev) => {
    if (ev.target.matches("[name='enderecoId'], [name='endereco_estado'], [name='cupomPromocional']"))
      agendarResumo();
  };
  form.addEventListener("input", aoAlterarResumo);
  form.addEventListener("change", aoAlterarResumo);
  agendarResumo();

  const chaveCheckout = crypto.randomUUID();
  form.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    if (form.dataset.enviando || estadoResumo.dataset.situacao === "calculando") return;
    form.dataset.enviando = "true";
    const botao = form.querySelector("[data-testid='finalizar-compra']");
    botao.disabled = true;
    form.querySelectorAll("[data-form-error]").forEach((x) => x.remove());
    try {
      const pedido = await vendasService.finalizar(id, montarPayload(form, cliente), chaveCheckout);
      toast("Pedido finalizado com status EM PROCESSAMENTO.");
      navegar(`/checkout/confirmacao/${pedido.id}`);
    } catch (err) {
      form.insertAdjacentHTML(
        "afterbegin",
        `<div data-form-error data-testid="erro-api" class="alert alert-danger" role="alert">${e(
          err.message,
        )}</div>`,
      );
      delete form.dataset.enviando;
      botao.disabled = false;
    }
  });
}
