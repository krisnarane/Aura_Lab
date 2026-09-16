import { criarNavegacaoAdmin } from "./adminLayout.js";
/**
 * Constrói a marcação das telas sem registrar eventos ou alterar o estado.
 * A camada de aplicação liga os comportamentos depois de cada renderização.
 */
import {
  state,
  products,
  product,
  available,
  cart,
  cartSubtotal,
  reservationRemaining,
  currentCustomer,
  customer,
  customers,
  ordersForCustomer,
  order,
  exchange,
  exchangesForCustomer,
  customerCoupons,
  checkoutTotals,
  selectedAddress,
  promotionalCoupons,
  exchangeCoupons,
  filterCustomers,
  filterOrders,
  analyticsCategories,
  salesAnalysis,
  cardBrands,
  customerRanking,
} from "./store.js";
import { escaparHtml, formatarMoeda } from "./utilitarios.js";

// Aliases mantidos temporariamente para preservar o contrato dos testes existentes.
export { formatarMoeda as money, escaparHtml as e };

// Transforma status técnicos como EM_ABERTO em textos mais legíveis para a tela.
export const exibirStatus = (value) =>
  String(value || "")
    .replaceAll("_", " ")
    .replace("EM TRANSITO", "EM TRÂNSITO");

// Cria o selo visual de status usado em pedidos, clientes, cupons e trocas.
export const status = (value) =>
  `<span class="status ${["ENTREGUE", "TROCA_ACEITA", "ITEM_RECEBIDO", "TROCA_PROCESSADA", "ATIVO", "VISIVEL", "PAGAMENTO_REALIZADO"].includes(value) ? "ok" : ""}">${exibirStatus(value)}</span>`;

// Resume a disponibilidade para que o catálogo mostre mensagens simples.
const descreverDisponibilidade = (item) =>
  available(item) <= 0
    ? "Esgotado"
    : available(item) < 8
      ? "Poucas unidades"
      : "Disponível";
const art = (item) =>
  `data-cor-produto="${escaparHtml(item.art || "#b65d78")}"`;

// Produto pode ter imagem cadastrada ou cair no bloco de arte com cor de apoio.
const criarArteProduto = (item) =>
  `<div class="product-art" ${art(item)}>${item.image ? `<img src="${escaparHtml(item.image)}" alt="${escaparHtml(item.name)}" data-ocultar-imagem-com-erro>` : ""}<span class="product-mark">aura</span></div>`;
const card = (item) =>
  `<article class="aura-card">${criarArteProduto(item)}<div class="card-body"><small>${escaparHtml(item.brand)}</small><button class="product-link" data-product="${item.id}">${escaparHtml(item.name)}</button><p class="availability ${available(item) <= 0 ? "sold-out" : ""}">${descreverDisponibilidade(item)}</p><div class="d-flex align-items-center"><b>${formatarMoeda(item.price)}</b><button class="round ms-auto" data-add="${item.id}" aria-label="Adicionar ${escaparHtml(item.name)}" ${available(item) > 0 ? "" : "disabled"}>＋</button></div></div></article>`;

export const header = () => {
  // O cabeçalho decide quais links aparecem a partir do cliente atual.
  // Quando o perfil da loja está conectado à API, o nome real tem precedência na identificação.
  const owner = currentCustomer();
  const nomeApi =
    typeof localStorage !== "undefined"
      ? localStorage.getItem("aura-lab-perfil-api-nome")
      : null;
  const path = typeof location !== "undefined" ? location.pathname : "/";
  const isActive = (href) => {
    if (href === "/")
      return (
        path === "/" || path === "/catalogo" || path.startsWith("/produto")
      );
    return path === href;
  };
  const navItems = [
    ["/", "Produtos"],
    ["/consultora", "Consultora Aura"],
    ...(owner ? [["/perfil/cupons", "Meus cupons"]] : []),
    ...(!owner ? [["/cadastro", "Cadastrar-se"]] : []),
  ];
  const navHtml = navItems
    .map(
      ([href, label]) =>
        `<li><a href="${href}" data-link class="${isActive(href) ? "active" : ""}">${label}</a></li>`,
    )
    .join("");
  return `<header class="site-header"><div class="notice">Beleza que acompanha
o seu ritmo <span>•</span> </div><nav class="navbar navbar-expand-lg container aura-container"><a href="/" data-link class="brand">aura <i>LAB</i></a><button class="navbar-toggler border-0" data-bs-toggle="collapse" data-bs-target="#nav" aria-label="Abrir menu">☰</button><div id="nav" class="collapse navbar-collapse"><ul class="navbar-nav mx-auto nav-main">${navHtml}</ul><div class="nav-actions"><a href="${owner || nomeApi ? "/perfil/dados" : "/cadastro"}" data-link><svg class="profile-icon" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3.5"/><path d="M4.5 20c.8-4 3.3-6 7.5-6s6.7 2 7.5 6"/></svg><span>${escaparHtml(nomeApi?.split(" ")[0] || owner?.name?.split(" ")[0] || "Cadastrar-se")}</span></a><a href="/sacola" data-link><svg class="profile-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 7a3 3 0 016 0v1"/><path d="M6 8h12l-1 13H7L6 8z"/></svg><span>Sacola</span><b>${cart().reduce((sum, item) => sum + item.q, 0)}</b></a></div></div></nav></header>`;
};

// Rodapé compartilhado pelas telas públicas e de cliente.
export const footer = () =>
  `<footer class="site-footer"><div class="container aura-container"><div class="row g-4"><div class="col-md-5"><a href="/" data-link class="brand light">aura <i>LAB</i></a><p>Beleza em intenção, produtos em conexão.</p></div><div class="col-6 col-md"><h4>Descubra</h4><a href="/" data-link>Produtos</a><a href="/consultora" data-link>Consultora Aura</a></div><div class="col-6 col-md"><h4>Minha Aura</h4><a href="/perfil/dados" data-link>Meu perfil</a><a href="/perfil/pedidos" data-link>Pedidos</a><a href="/perfil/cupons" data-link>Cupons</a></div><div class="col-md"><h4>Gestão</h4><a href="/admin/dashboard" data-link>Área administrativa</a></div></div><div class="footer-bottom">© 2026 Aura Lab</div></div></footer>`;

const criarLayoutCliente = (content) => `${header()}${content}${footer()}`;

// Abas do perfil reaproveitam a mesma marcação e só mudam a aba ativa.
const criarNavegacaoPerfil = (active) =>
  `<nav class="profile-tabs">${[
    ["Dados", "/perfil/dados", "dados"],
    ["Segurança", "/perfil/seguranca", "seguranca"],
    ["Endereços", "/perfil/enderecos", "enderecos"],
    ["Cartões", "/perfil/cartoes", "cartoes"],
    ["Pedidos", "/perfil/pedidos", "pedidos"],
    ["Cupons", "/perfil/cupons", "cupons"],
  ]
    .map(
      ([label, href, key]) =>
        `<a href="${href}" data-link class="${active === key ? "active" : ""}">${label}</a>`,
    )
    .join("")}</nav>`;
const criarEstruturaPerfil = (active, title, content) =>
  criarLayoutCliente(
    `<main class="page"><div class="container aura-container page-top"><span class="eyebrow">Cliente cadastrado</span><h1>${title}</h1>${criarNavegacaoPerfil(active)}<div class="profile-content wide">${content}</div></div></main>`,
  );
const criarPerfilConvidado = () =>
  criarLayoutCliente(
    `<main class="page"><div class="container aura-container page-top"><span class="eyebrow">Área do cliente</span><h1>Cliente demonstrativo não identificado.</h1><div class="panel"><p>Restaure a massa de demonstração para voltar a usar Marina Costa.</p></div></div></main>`,
  );

export function catalog(query) {
  // Lê filtros da URL para permitir compartilhar ou recarregar a busca atual.
  const category = query.get("category") || "Todos";
  const search = (query.get("q") || "").toLowerCase();
  const selected = (key) => query.get(key) || "";
  const visible = products();
  const unique = (fn) => [
    ...new Set(visible.map(fn).filter((value) => value && value !== "—")),
  ];

  // Cria selects de filtro usando valores presentes no próprio catálogo.
  const options = (key, values, label) =>
    `<label class="mt-3">${label}<select class="form-select mt-1" data-filter="${key}"><option value="">Todos</option>${values.map((value) => `<option value="${escaparHtml(value)}" ${selected(key) === value ? "selected" : ""}>${escaparHtml(value)}</option>`).join("")}</select></label>`;
  const list = visible.filter(
    (item) =>
      (category === "Todos" || item.categories.includes(category)) &&
      (!selected("brand") || item.brand === selected("brand")) &&
      (!selected("finish") || item.finish === selected("finish")) &&
      (!selected("skin") || item.skin === selected("skin")) &&
      `${item.name} ${item.brand} ${item.ingredients}`
        .toLowerCase()
        .includes(search),
  );

  // O retorno é uma string HTML; os eventos serão ligados depois em app.js.
  return criarLayoutCliente(
    `<main class="page"><div class="container aura-container catalog-content"><div class="row g-4"><aside class="col-lg-3"><div class="panel filters"><div class="d-flex justify-content-between"><b>Filtros</b><a href="/" data-link>Limpar</a></div><hr>${["Todos", "Face", "Olhos", "Lábios", "Skincare"].map((value) => `<label><input type="radio" name="cat" value="${value}" ${value === category ? "checked" : ""}> ${value}</label>`).join("")}${options(
      "brand",
      unique((item) => item.brand),
      "Marca",
    )}${options(
      "finish",
      unique((item) => item.finish),
      "Acabamento",
    )}${options(
      "skin",
      unique((item) => item.skin),
      "Tipo de pele",
    )}</div></aside><section class="col-lg-9"><div class="catalog-search"><label for="search">Encontre seu próximo ritual</label><div class="input-group"><span class="input-group-text">⌕</span><input id="search" class="form-control" value="${escaparHtml(search)}" placeholder="Buscar por produto, marca ou ingrediente"></div></div><div class="catalog-heading"><p class="muted">${list.length} produtos encontrados</p><span>Ordenar por <b>Mais relevantes</b></span></div><div class="row g-3">${list.map((item) => `<div class="col-sm-6 col-xl-4">${card(item)}</div>`).join("") || '<div class="panel">Nenhum produto encontrado.</div>'}</div></section></div></div></main>`,
  );
}

export function detail(id) {
  // Se o produto não existir ou estiver oculto, volta para o catálogo.
  const item = product(id);
  if (!item || !item.visible) return catalog(new URLSearchParams());
  return criarLayoutCliente(
    `<main class="page"><div class="container aura-container"><a href="/" data-link class="back">← Produtos</a><div class="row g-5 mt-1 align-items-center"><div class="col-lg-6"><div class="detail-art" ${art(item)}>${item.image ? `<img src="${escaparHtml(item.image)}" alt="${escaparHtml(item.name)}" data-ocultar-imagem-com-erro>` : ""}<span class="product-mark">aura</span></div></div><div class="col-lg-6"><small>${escaparHtml(item.brand)}</small><h1>${escaparHtml(item.name)}</h1><h3>${formatarMoeda(item.price)}</h3><p>${escaparHtml(item.description)}</p><div class="chips"><span>${descreverDisponibilidade(item)}</span><span>${escaparHtml(item.tone)}</span><span>${escaparHtml(item.finish)}</span><span>${escaparHtml(item.volume)}</span></div><div class="ingredients"><b>Ingredientes em destaque</b><p>${escaparHtml(item.ingredients)}</p></div><div class="product-buy"><label for="product-quantity">Quantidade</label><input id="product-quantity" class="form-control" type="number" min="1" max="${item.stock}" value="1"><button class="btn aura-btn" data-add-detail="${item.id}" ${available(item) > 0 ? "" : "disabled"}>${available(item) > 0 ? "Adicionar à sacola" : "Indisponível"}</button></div></div></div></div></main>`,
  );
}

export function cartPage() {
  // A sacola recalcula reserva expirada quando cart() é chamada.
  const items = cart();
  const sub = cartSubtotal();
  const expired = state.expiredItems.length
    ? `<div class="expired-items"><b>Itens removidos após expiração:</b> ${state.expiredItems.map(e).join(", ")}.</div>`
    : "";
  return criarLayoutCliente(
    `<main class="page"><div class="container aura-container page-top"><span class="eyebrow">Sua sacola</span><h1>Seu ritual está quase pronto.</h1>${expired}${items.length ? `<div class="row g-4"><section class="col-lg-8 panel"><div class="reservation">◷ Reserva ativa por aproximadamente <b>${reservationRemaining()} minutos</b>.</div>${items.map(({ p, q }) => `<div class="cart-row"><div><small>${escaparHtml(p.brand)}</small><b>${escaparHtml(p.name)}</b><span class="muted">${descreverDisponibilidade(p)}</span></div><div class="quantity"><button data-qty="${p.id}" data-d="-1" aria-label="Diminuir">−</button><span>${q}</span><button data-qty="${p.id}" data-d="1" aria-label="Aumentar">+</button></div><b>${formatarMoeda(p.price * q)}</b><button class="link-danger" data-remove-cart="${p.id}">Remover</button></div>`).join("")}</section><aside class="col-lg-4"><div class="panel summary"><h3>Resumo</h3><p>Produtos <b>${formatarMoeda(sub)}</b></p><p>Frete <b>calculado pelo endereço</b></p><h3>Subtotal ${formatarMoeda(sub)}</h3><a href="/checkout/endereco" data-link class="btn aura-btn w-100">Iniciar checkout</a></div></aside></div>` : `<div class="panel text-center">Sua sacola está vazia. <a href="/" data-link>Explorar produtos</a></div>`}</div></main>`,
  );
}

// Etapas visuais do checkout aparecem em endereço, pagamento e revisão.
const criarEtapasCheckout = (active) =>
  `<ol class="checkout-steps">${[
    ["endereco", "1", "Endereço"],
    ["pagamento", "2", "Pagamento"],
    ["revisao", "3", "Revisão"],
  ]
    .map(
      ([key, number, label]) =>
        `<li class="${active === key ? "active" : ""}"><span>${number}</span>${label}</li>`,
    )
    .join("")}</ol>`;
const criarResumoCheckout = (totals) =>
  `<aside class="col-lg-4"><div class="panel summary payment-summary"><h3>Resumo</h3><p>Produtos <b>${formatarMoeda(totals.subtotal)}</b></p><p>Desconto <b>− ${formatarMoeda(totals.discount)}</b></p><p>Frete <b>${formatarMoeda(totals.shipping)}</b></p><p>Cupons de troca <b>− ${formatarMoeda(totals.exchangeApplied)}</b></p><p>Cartões <b>${formatarMoeda(totals.cardPaid)}</b></p><hr><h3>Total ${formatarMoeda(totals.total)}</h3><p class="${Math.abs(totals.difference) < 0.01 ? "text-success" : "text-destructive"}">Restante: <b>${formatarMoeda(totals.difference)}</b></p></div></aside>`;
const descreverFinalidades = (item) =>
  [
    item.purposes?.includes("COBRANCA") ? "Cobrança" : "",
    item.purposes?.includes("ENTREGA") ? "Entrega" : "",
  ]
    .filter(Boolean)
    .join(" e ");
const formatarEnderecoCompleto = (item) =>
  `${item.streetType ? escaparHtml(item.streetType) + " " : ""}${escaparHtml(item.street)}, ${escaparHtml(item.number)} · ${escaparHtml(item.district)} · ${escaparHtml(item.city)}/${escaparHtml(item.state)}`;
const criarCamposEndereco = (
  item = {},
  labelName = "label",
  required = true,
) => {
  // O mesmo conjunto de campos atende cadastro, perfil e checkout.
  const req = required ? "required" : "";
  return `<div class="row g-3"><div class="col-md-6"><label>Apelido<input class="form-control" name="${labelName}" value="${escaparHtml(item.label || "")}" placeholder="Ex.: Casa" ${req}></label></div><div class="col-md-6"><label>Tipo de residência<select class="form-select" name="residenceType" ${req}><option value="">Selecione</option>${["Casa", "Apartamento", "Comercial", "Outro"].map((value) => `<option ${item.residenceType === value ? "selected" : ""}>${value}</option>`).join("")}</select></label></div><div class="col-md-4"><label>Tipo de logradouro<select class="form-select" name="streetType" ${req}><option value="">Selecione</option>${["Rua", "Avenida", "Alameda", "Travessa", "Rodovia", "Praça"].map((value) => `<option ${item.streetType === value ? "selected" : ""}>${value}</option>`).join("")}</select></label></div><div class="col-md-8"><label>Logradouro<input class="form-control" name="street" value="${escaparHtml(item.street || "")}" ${req}></label></div><div class="col-md-4"><label>Número<input class="form-control" name="number" value="${escaparHtml(item.number || "")}" ${req}></label></div><div class="col-md-4"><label>Bairro<input class="form-control" name="district" value="${escaparHtml(item.district || "")}" ${req}></label></div><div class="col-md-4"><label>CEP<input class="form-control" name="cep" value="${escaparHtml(item.cep || "")}" ${req}></label></div><div class="col-md-5"><label>Cidade<input class="form-control" name="city" value="${escaparHtml(item.city || "")}" ${req}></label></div><div class="col-md-3"><label>Estado<input class="form-control" name="state" maxlength="2" value="${escaparHtml(item.state || "")}" ${req}></label></div><div class="col-md-4"><label>País<input class="form-control" name="country" value="${escaparHtml(item.country || "Brasil")}" ${req}></label></div><div class="col-12"><label>Observações <small>(opcional)</small><textarea class="form-control" name="observations" rows="2">${escaparHtml(item.observations || "")}</textarea></label></div></div>`;
};

// Mantém a lista de bandeiras sincronizada com o store.
const criarOpcoesBandeira = (selected) =>
  cardBrands
    .map(
      (value) =>
        `<option value="${escaparHtml(value)}" ${selected === value ? "selected" : ""}>${escaparHtml(value)}</option>`,
    )
    .join("");
const criarFormularioCartao = (id = "card-form", hidden = false) =>
  `<form id="${id}" class="panel form-stack mt-3 ${hidden ? "hidden-section" : ""}"><h3>Novo cartão</h3><p class="muted small">O número completo e o código de segurança são validados somente durante o preenchimento; o protótipo salva apenas bandeira, nome impresso e quatro últimos dígitos.</p><div class="row g-3"><div class="col-md-6"><label>Número do cartão<input class="form-control" name="number" inputmode="numeric" minlength="13" maxlength="23" required></label></div><div class="col-md-6"><label>Nome impresso no cartão<input class="form-control" name="holder" required></label></div><div class="col-md-6"><label>Bandeira<select class="form-select" name="brand" required><option value="">Selecione</option>${criarOpcoesBandeira("")}</select></label></div><div class="col-md-6"><label>Código de segurança<input class="form-control" name="securityCode" type="password" inputmode="numeric" minlength="3" maxlength="4" required></label></div></div><label class="form-check"><input class="form-check-input" type="checkbox" name="preferred"> Tornar preferencial</label><button class="btn aura-btn">Adicionar e salvar cartão</button></form>`;

export function checkoutAddress() {
  // Mostra endereços de entrega ativos e a opção de cadastrar um endereço novo.
  const owner = currentCustomer();
  const current =
    state.checkout.addressId || (state.checkout.newAddress ? "new" : "");
  const totals = checkoutTotals();
  const deliveryAddresses = (owner?.addresses || []).filter(
    (item) => item.active && item.purposes?.includes("ENTREGA"),
  );
  if (!owner) return criarPerfilConvidado();
  const useNewAddress = current === "new";
  return criarLayoutCliente(
    `<main class="page"><div class="container aura-container page-top"><span class="eyebrow">Checkout</span><h1>Onde você quer receber?</h1>${criarEtapasCheckout("endereco")}<div class="row g-4"><section class="col-lg-8"><form id="checkout-address-form" class="panel form-stack"><h2>Endereço de entrega</h2>${deliveryAddresses.map((item) => `<label class="choice-card"><input type="radio" name="addressId" value="${item.id}" ${current === item.id ? "checked" : ""} required><span><b>${escaparHtml(item.label)}</b><small>${formatarEnderecoCompleto(item)}</small></span>${item.preferred ? "<em>Preferencial</em>" : ""}</label>`).join("")}<label class="choice-card"><input type="radio" name="addressId" value="new" ${useNewAddress ? "checked" : ""}><span><b>Usar novo endereço</b><small>Preencha os dados obrigatórios abaixo.</small></span></label><div class="new-address-fields ${useNewAddress ? "" : "hidden-section"}">${criarCamposEndereco(state.checkout.newAddress || {}, "label")}<label class="form-check mt-3"><input class="form-check-input" type="checkbox" name="saveAddress"> Salvar como endereço de entrega no perfil</label></div><button class="btn aura-btn">Continuar para pagamento</button></form></section>${criarResumoCheckout(totals)}</div></div></main>`,
  );
}

export function checkoutPayment() {
  // Exibe cupons e cartões disponíveis para compor o pagamento.
  const owner = currentCustomer();
  if (!owner) return criarPerfilConvidado();
  const totals = checkoutTotals();
  const selectedPromo = state.coupons.find(
    (item) => item.id === state.checkout.promoCouponId,
  );
  const exchangeOptions = exchangeCoupons()
        .map(
          (item) =>
            `<label class="choice-card"><input type="checkbox" name="exchangeCoupon" value="${item.id}" ${state.checkout.exchangeCouponIds.includes(item.id) ? "checked" : ""} ${item.active && item.balance > 0 ? "" : "disabled"}><span><b>${escaparHtml(item.code)}</b><small>Saldo ${formatarMoeda(item.balance)} · validade ${escaparHtml(item.validUntil)}</small></span></label>`,
        )
        .join("") || "<p>Nenhum cupom de troca disponível.</p>";

  // Cada cartão ativo recebe um campo para informar quanto será cobrado nele.
  const paymentFields = owner.cards.filter((item) => item.active).map((item) => `<label class="payment-card"><span><b>${escaparHtml(item.brand || item.label)} final ${escaparHtml(item.last4)}</b><small>${escaparHtml(item.holder || "")} ${item.preferred ? "· Preferencial" : ""}</small></span><input class="form-control" type="number" min="0" step="0.01" name="card_${item.id}" value="${state.checkout.cardAllocations[item.id] || ""}" placeholder="R$ 0,00"></label>`).join("");
  return criarLayoutCliente(
    `<main class="page"><div class="container aura-container page-top"><span class="eyebrow">Checkout</span><h1>Combine suas formas de pagamento.</h1>${criarEtapasCheckout("pagamento")}<div class="row g-4"><section class="col-lg-8"><form id="checkout-payment-form" class="panel form-stack"><h2>Cupons</h2><label>Cupom promocional<div class="input-group"><input class="form-control" name="promoCode" value="${escaparHtml(selectedPromo?.code || "")}" placeholder="Ex.: AURA10"><button type="button" class="btn btn-outline-secondary" data-apply-payment>Aplicar e recalcular</button></div></label><p class="muted small">Um cupom promocional reduz o preço. Cupons de troca funcionam como crédito.</p><div class="choice-grid">${exchangeOptions}</div><div class="section-heading"><div><h2>Cartões</h2><p class="muted">Informe quanto será cobrado para concluir a compra.</p></div><button type="button" class="btn btn-outline-secondary" data-toggle-new-card>Adicionar cartão</button></div>${paymentFields}<div class="d-flex gap-2 flex-wrap"><button type="button" class="btn btn-outline-secondary" data-auto-allocate>Colocar restante no preferencial</button><button class="btn aura-btn">Salvar pagamento e revisar</button></div></form>${criarFormularioCartao("checkout-new-card-form", true)}</section>${criarResumoCheckout(totals)}</div></div></main>`,
  );
}

export function checkoutReview() {
  // Consolida itens, entrega e pagamento para a confirmação final do pedido.
  const totals = checkoutTotals();
  const address = selectedAddress();
  const owner = currentCustomer();
  return criarLayoutCliente(
    `<main class="page"><div class="container aura-container page-top"><span class="eyebrow">Checkout</span><h1>Revise antes de finalizar.</h1>${criarEtapasCheckout("revisao")}<div class="row g-4"><section class="col-lg-8"><div class="panel"><div class="review-block"><div><h3>Itens</h3>${cart()
      .map(
        (line) =>
          `<p>${escaparHtml(line.p.name)} × ${line.q} <b>${formatarMoeda(line.p.price * line.q)}</b></p>`,
      )
      .join(
        "",
      )}</div><a href="/sacola" data-link>Editar</a></div><div class="review-block"><div><h3>Entrega</h3><p><b>${escaparHtml(address?.label)}</b><br>${escaparHtml(address?.street)}, ${escaparHtml(address?.number)} · ${escaparHtml(address?.city)}/${escaparHtml(address?.state)}</p></div><a href="/checkout/endereco" data-link>Editar</a></div><div class="review-block"><div><h3>Pagamento</h3>${totals.promo ? `<p>Promocional ${escaparHtml(totals.promo.code)}: − ${formatarMoeda(totals.discount)}</p>` : ""}${totals.selectedTrades.map((item) => `<p>Cupom ${escaparHtml(item.code)}: saldo ${formatarMoeda(item.balance)}</p>`).join("")}${Object.entries(
      state.checkout.cardAllocations,
    )
      .map(([id, value]) => {
        const cardItem = owner.cards.find((item) => item.id === id);
        return `<p>${escaparHtml(cardItem?.label)} final ${escaparHtml(cardItem?.last4)}: ${formatarMoeda(value)}</p>`;
      })
      .join(
        "",
      )}</div><a href="/checkout/pagamento" data-link>Editar</a></div><div class="alert-soft"><b>Ao finalizar, o pedido será criado como EM ABERTO.</b> Pagamento, frete e entrega são simulações acadêmicas.</div><button class="btn aura-btn" data-finalize-order ${Math.abs(totals.difference) < 0.01 && address && cart().length ? "" : "disabled"}>Finalizar pedido</button></div></section>${criarResumoCheckout(totals)}</div></div></main>`,
  );
}

export function checkoutConfirmation() {
  // Usa o último pedido salvo no checkout para montar a confirmação.
  const item = order(state.checkout.lastOrderId);
  const owner = currentCustomer();
  const trackingLink = item?.customerId === owner?.id
    ? `<a href="/perfil/pedidos/${encodeURIComponent(item.id)}" data-link class="btn aura-btn">Acompanhar pedido</a>`
    : "";
  return criarLayoutCliente(
    `<main class="page"><div class="container aura-container page-top"><div class="panel confirmation"><span class="confirmation-mark">✓</span><span class="eyebrow">Pedido recebido</span><h1>Obrigada, ${escaparHtml(owner?.name?.split(" ")[0] || "Marina")}.</h1><p>Seu pedido <b>${escaparHtml(item?.id || "")}</b> foi criado com status ${status(item?.status || "EM_ABERTO")}.</p><div class="d-flex gap-2 justify-content-center flex-wrap">${trackingLink}<a href="/" data-link class="btn btn-outline-secondary">Continuar comprando</a></div></div></div></main>`,
  );
}

export function registrationPage() {
  // Formulário inicial cria cliente e endereço residencial em uma única etapa.
  return criarLayoutCliente(
    `<main class="page"><div class="container aura-container page-top"><span class="eyebrow">Cadastro de cliente</span><h1>Crie seu perfil Aura.</h1><form id="customer-create-form" class="panel form-stack"><h2>Dados pessoais</h2><div class="row g-3"><div class="col-md-6"><label>Nome completo<input class="form-control" name="name" required></label></div><div class="col-md-6"><label>CPF<input class="form-control" name="cpf" required></label></div><div class="col-md-6"><label>E-mail<input class="form-control" name="email" type="email" required></label></div><div class="col-md-3"><label>Data de nascimento<input class="form-control" name="birthDate" type="date" required></label></div><div class="col-md-3"><label>Gênero<select class="form-select" name="gender" required><option value="">Selecione</option><option>Feminino</option><option>Masculino</option><option>Não binário</option><option>Outro</option><option>Prefiro não informar</option></select></label></div><div class="col-md-3"><label>Tipo de telefone<select class="form-select" name="phoneType" required><option>Celular</option><option>Residencial</option><option>Comercial</option></select></label></div><div class="col-md-3"><label>DDD<input class="form-control" name="phoneDdd" inputmode="numeric" maxlength="2" required></label></div><div class="col-md-6"><label>Número do telefone<input class="form-control" name="phoneNumber" required></label></div><div class="col-md-6"><label>Senha demonstrativa<input class="form-control" name="password" type="password" minlength="6" required></label></div></div><hr><h2>Endereço residencial</h2><p class="muted small">O primeiro endereço será registrado para cobrança e entrega.</p>${criarCamposEndereco({}, "addressLabel", true)}<button class="btn aura-btn">Criar perfil</button></form></div></main>`,
  );
}

export function profileData() {
  // Tela de dados pessoais do cliente demonstrativo atual.
  const owner = currentCustomer();
  if (!owner) return criarPerfilConvidado();
  return criarEstruturaPerfil(
    "dados",
    "Meus dados",
    `<form id="customer-profile-form" class="panel form-stack"><div class="section-heading"><div><h2>Dados cadastrais</h2><p>${status(owner.active ? "ATIVO" : "INATIVO")}</p></div><div><span class="muted small">Ranking de compras</span><h2>${customerRanking(owner.id)} / 5</h2></div></div><div class="row g-3"><div class="col-md-6"><label>Nome completo<input class="form-control" name="name" value="${escaparHtml(owner.name)}" required></label></div><div class="col-md-6"><label>CPF<input class="form-control" name="cpf" value="${escaparHtml(owner.cpf)}" required></label></div><div class="col-md-6"><label>E-mail<input class="form-control" name="email" type="email" value="${escaparHtml(owner.email)}" required></label></div><div class="col-md-3"><label>Data de nascimento<input class="form-control" name="birthDate" type="date" value="${escaparHtml(owner.birthDate)}" required></label></div><div class="col-md-3"><label>Gênero<select class="form-select" name="gender" required>${["Feminino", "Masculino", "Não binário", "Outro", "Prefiro não informar"].map((value) => `<option ${owner.gender === value ? "selected" : ""}>${value}</option>`).join("")}</select></label></div><div class="col-md-3"><label>Tipo de telefone<select class="form-select" name="phoneType" required>${["Celular", "Residencial", "Comercial"].map((value) => `<option ${owner.phoneType === value ? "selected" : ""}>${value}</option>`).join("")}</select></label></div><div class="col-md-3"><label>DDD<input class="form-control" name="phoneDdd" inputmode="numeric" maxlength="2" value="${escaparHtml(owner.phoneDdd)}" required></label></div><div class="col-md-6"><label>Número do telefone<input class="form-control" name="phoneNumber" value="${escaparHtml(owner.phoneNumber)}" required></label></div></div><div class="d-flex justify-content-between gap-2 flex-wrap"><button class="btn aura-btn" ${owner.active ? "" : "disabled"}>Salvar alterações</button><button type="button" class="btn btn-outline-danger" data-inactivate-customer ${owner.active ? "" : "disabled"}>Inativar minha conta</button></div></form>`,
  );
}

export function profileSecurity() {
  // Tela isolada para alteração da senha demonstrativa.
  const owner = currentCustomer();
  if (!owner) return criarPerfilConvidado();
  return criarEstruturaPerfil(
    "seguranca",
    "Segurança",
    `<form id="password-form" class="panel form-stack"><h2>Alterar senha</h2><p class="muted">Senha demonstrativa, sem autenticação real.</p><label>Senha atual<input class="form-control" type="password" name="currentPassword" required></label><label>Nova senha<input class="form-control" type="password" name="newPassword" minlength="6" required></label><label>Confirmar nova senha<input class="form-control" type="password" name="confirmPassword" minlength="6" required></label><button class="btn aura-btn">Atualizar senha</button></form>`,
  );
}

// Formulário de endereço usado para cadastro e edição dentro do perfil.
const criarFormularioEndereco = (item) =>
  `<form id="address-form" class="panel form-stack mt-3"><input type="hidden" name="addressId" value="${escaparHtml(item?.id || "")}"><div class="section-heading"><div><h3>${item ? "Editar endereço" : "Novo endereço"}</h3><p class="muted small">Todos os campos são obrigatórios, exceto observações.</p></div>${item ? '<a href="/perfil/enderecos" data-link>Cancelar edição</a>' : ""}</div>${criarCamposEndereco(item || {})}<div class="choice-grid"><label class="choice-card"><input type="checkbox" name="billing" ${item ? (item.purposes?.includes("COBRANCA") ? "checked" : "") : "checked"}><span><b>Endereço de cobrança</b><small>Usado para faturamento.</small></span></label><label class="choice-card"><input type="checkbox" name="delivery" ${item ? (item.purposes?.includes("ENTREGA") ? "checked" : "") : "checked"}><span><b>Endereço de entrega</b><small>Pode ser selecionado no checkout.</small></span></label></div><label class="form-check"><input class="form-check-input" type="checkbox" name="preferred" ${item?.preferred ? "checked" : ""}> Tornar preferencial para entrega</label><button class="btn aura-btn">${item ? "Salvar alterações" : "Cadastrar endereço"}</button></form>`;
export function profileAddresses(query = new URLSearchParams()) {
  // Query string define se a tela está cadastrando ou editando um endereço.
  const owner = currentCustomer();
  if (!owner) return criarPerfilConvidado();
  const editing =
    owner.addresses.find((item) => item.id === query.get("edit")) || null;
  const list = owner.addresses
    .map(
      (item) =>
        `<article class="management-card ${item.active ? "" : "is-inactive"}"><div><h3>${escaparHtml(item.label)} ${item.preferred ? "<small>Preferencial</small>" : ""}</h3><p><b>${escaparHtml(descreverFinalidades(item) || "Sem finalidade")}</b><br>${formatarEnderecoCompleto(item)}<br>CEP ${escaparHtml(item.cep)} · ${escaparHtml(item.country)}${item.observations ? "<br>" + escaparHtml(item.observations) : ""}</p></div><div class="management-actions">${item.active ? `<a href="/perfil/enderecos?edit=${encodeURIComponent(item.id)}" data-link>Editar</a>` : ""}${item.active && item.purposes?.includes("ENTREGA") && !item.preferred ? `<button data-preferred-address="${item.id}">Tornar preferencial</button>` : ""}${item.active ? `<button data-inactivate-address="${item.id}">Inativar</button>` : status("INATIVO")}</div></article>`,
    )
    .join("");
  return criarEstruturaPerfil(
    "enderecos",
    "Meus endereços",
    `<div class="panel"><div class="section-heading"><div><h2>Endereços cadastrados</h2><p class="muted">O cliente deve manter ao menos um endereço de cobrança e um de entrega.</p></div></div>${list || "<p>Nenhum endereço cadastrado.</p>"}</div>${criarFormularioEndereco(editing)}`,
  );
}

export function profileCards() {
  // Lista cartões mascarados e permite marcar preferência ou inativar.
  const owner = currentCustomer();
  if (!owner) return criarPerfilConvidado();
  const list = owner.cards
    .map(
      (item) =>
        `<article class="management-card ${item.active ? "" : "is-inactive"}"><div><h3>${escaparHtml(item.brand || item.label)} final ${escaparHtml(item.last4)} ${item.preferred ? "<small>Preferencial</small>" : ""}</h3><p>Nome impresso: ${escaparHtml(item.holder || "Não informado")}<br>O número completo e o código de segurança não ficam armazenados.</p></div><div class="management-actions">${item.active && !item.preferred ? `<button data-preferred-card="${item.id}">Tornar preferencial</button>` : ""}${item.active ? `<button data-inactivate-card="${item.id}">Inativar</button>` : status("INATIVO")}</div></article>`,
    )
    .join("");
  return criarEstruturaPerfil(
    "cartoes",
    "Meus cartões",
    `<div class="panel"><h2>Cartões cadastrados</h2>${list || "<p>Nenhum cartão cadastrado.</p>"}</div>${criarFormularioCartao()}`,
  );
}

// Transforma os itens de um pedido em um resumo curto para a lista do perfil.
const descreverItensPedido = (item) =>
  item.items
    .map(
      (line) =>
        `${escaparHtml(product(line.productId)?.name || "Produto")} (${line.quantity})`,
    )
    .join(", ");
export function profileOrders() {
  // Reúne pedidos e trocas do cliente em uma visão de acompanhamento.
  const owner = currentCustomer();
  if (!owner) return criarPerfilConvidado();
  const list = ordersForCustomer(owner.id);
  return criarEstruturaPerfil(
    "pedidos",
    "Meus pedidos",
    `<div class="panel">${list.map((item) => `<article class="order"><div><b>${escaparHtml(item.id)}</b><span>${escaparHtml(item.date)} · ${descreverItensPedido(item)} · ${formatarMoeda(item.total)}</span></div>${status(item.status)}<a href="/perfil/pedidos/${encodeURIComponent(item.id)}" data-link class="row-action">Ver detalhes</a></article>`).join("") || "<p>Nenhum pedido.</p>"}</div><div class="panel mt-3"><h2>Minhas trocas</h2>${
      exchangesForCustomer(owner.id)
        .map(
          (item) =>
            `<article class="order"><div><b>${escaparHtml(item.id)}</b><span>${escaparHtml(product(item.productId)?.name)} · pedido ${escaparHtml(item.orderId)}</span></div>${status(item.status)}${item.status === "TROCA_ACEITA" ? `<a href="/perfil/pedidos/${encodeURIComponent(item.orderId)}" data-link class="row-action">Informar despacho</a>` : ""}</article>`,
        )
        .join("") || "<p>Nenhuma troca.</p>"
    }</div>`,
  );
}

// Linha do tempo reaproveitada em detalhes de pedido do cliente e do administrador.
const criarLinhaDoTempo = (history) =>
  `<ol class="criarLinhaDoTempo">${history.map((item) => `<li><span></span><div><b>${exibirStatus(item.status)}</b><small>${new Date(item.at).toLocaleString("pt-BR")} · ${escaparHtml(item.actor)}</small></div></li>`).join("")}</ol>`;
export function profileOrderDetail(id) {
  // Garante que o cliente só veja detalhes dos próprios pedidos.
  const item = order(id);
  const owner = currentCustomer();
  if (!owner) return criarPerfilConvidado();
  if (!item || item.customerId !== owner.id) return notFound();
  const related = exchangesForCustomer(owner.id).filter(
    (value) => value.orderId === item.id,
  );

  // Ações do cliente dependem do status atual do pedido.
  const actions = `<div class="d-flex gap-2 flex-wrap">${["EM_ABERTO", "EM_PROCESSAMENTO"].includes(item.status) ? `<button class="btn btn-outline-danger" data-cancel-order="${escaparHtml(item.id)}">Cancelar pedido</button>` : ""}${item.status === "EM_TRANSITO" ? `<button class="btn aura-btn" data-confirm-receipt="${escaparHtml(item.id)}">Confirmar recebimento</button>` : ""}</div>`;
  const itemCards = item.items
    .map(
      (line) =>
        `<article class="order-item"><div><b>${escaparHtml(product(line.productId)?.name)}</b><span>Quantidade ${line.quantity} · ${formatarMoeda(line.unitPrice)}</span></div>${item.status === "ENTREGUE" ? `<form class="exchange-request-form" data-order-id="${escaparHtml(item.id)}" data-product-id="${line.productId}"><input class="form-control" type="number" name="quantity" min="1" max="${line.quantity}" value="1" aria-label="Quantidade para troca"><input class="form-control" name="reason" placeholder="Motivo da troca" required><button class="btn btn-outline-secondary">Solicitar troca</button></form>` : ""}</article>`,
    )
    .join("");
  const exchangeCards = related
    .map(
      (value) =>
        `<article class="management-card"><div><h3>${escaparHtml(product(value.productId)?.name)} · ${status(value.status)}</h3><p>${escaparHtml(value.reason)}${value.justification ? `<br><b>Justificativa:</b> ${escaparHtml(value.justification)}` : ""}</p>${value.tracking ? `<p>${escaparHtml(value.carrier)} · rastreio ${escaparHtml(value.tracking)}</p>` : ""}</div>${value.status === "TROCA_ACEITA" ? `<form class="exchange-dispatch-form" data-exchange-id="${value.id}"><input class="form-control" name="carrier" placeholder="Transportadora" required><input class="form-control" name="tracking" placeholder="Código de rastreio" required><button class="btn aura-btn">Informar despacho</button></form>` : ""}</article>`,
    )
    .join("");
  return criarEstruturaPerfil(
    "pedidos",
    `Pedido ${escaparHtml(item.id)}`,
    `<div class="row g-4"><section class="col-lg-8"><div class="panel"><div class="section-heading"><div><h2>Resumo</h2><p>${escaparHtml(item.date)} · ${formatarMoeda(item.total)}</p></div>${status(item.status)}</div>${itemCards}${actions}</div><div class="panel mt-3"><h2>Entrega</h2><p><b>${escaparHtml(item.address.label)}</b><br>${escaparHtml(item.address.street)}, ${escaparHtml(item.address.number)} · ${escaparHtml(item.address.city)}/${escaparHtml(item.address.state)}</p></div><div class="panel mt-3"><h2>Composição do pagamento</h2>${item.payments.map((payment) => `<p class="payment-line"><span>${escaparHtml(payment.label)}</span><b>${payment.type === "CUPOM_PROMOCIONAL" ? "− " : ""}${formatarMoeda(payment.amount)}</b></p>`).join("")}</div>${exchangeCards ? `<div class="panel mt-3"><h2>Trocas deste pedido</h2>${exchangeCards}</div>` : ""}</section><aside class="col-lg-4"><div class="panel"><h3>Linha do tempo</h3>${criarLinhaDoTempo(item.history)}</div></aside></div>`,
  );
}

export function profileCoupons() {
  // Separa cupons promocionais dos créditos gerados por troca.
  const owner = currentCustomer();
  if (!owner) return criarPerfilConvidado();
  const list = customerCoupons(owner.id);
  const promos = list.filter((item) => item.type === "PROMOCIONAL");
  const trades = list.filter((item) => item.type === "TROCA");
  return criarEstruturaPerfil(
    "cupons",
    "Meus cupons",
    `<div class="row g-4"><section class="col-lg-6 panel"><h2>Promocionais</h2>${promos.map((item) => `<article class="coupon-card"><b>${escaparHtml(item.code)}</b><span>${item.mode === "PERCENTUAL" ? `${item.value}% de desconto` : formatarMoeda(item.value)}</span><small>Validade ${escaparHtml(item.validUntil)} · ${item.active ? "Disponível" : "Indisponível"}</small></article>`).join("")}</section><section class="col-lg-6 panel"><h2>Cupons de troca</h2>${trades.map((item) => `<article class="coupon-card"><b>${escaparHtml(item.code)}</b><span>Saldo ${formatarMoeda(item.balance)}</span><small>Validade ${escaparHtml(item.validUntil)} · ${item.active ? "Disponível" : "Utilizado"}</small></article>`).join("") || "<p>Nenhum crédito de troca.</p>"}</section></div>`,
  );
}

// Tela do chat; a inteligência da resposta fica em consultora-aura.js.
export const advisorChat = () =>
  criarLayoutCliente(
    `<main class="page"><div class="container aura-container page-top"><section class="advisor chat-advisor"><div><span class="eyebrow">Consultora Aura</span><h1>Uma conversa, uma rotina mais sua.</h1><p></p></div><div id="aura-chat-messages" class="aura-chat" aria-live="polite"><div class="chat-message aura-message"><b>Aura</b><p>Olá! Conte seu tipo de pele ou o acabamento que procura.</p></div></div><div class="chat-composer"><input id="advisor-message" class="form-control" placeholder="Ex.: hidratação para pele sensível"><button id="advisor-send" class="btn aura-btn">Enviar</button></div></section></div></main>`,
  );

// Navegação lateral fixa da área administrativa.
const criarEstruturaAdmin = (active, title, content) =>
  `<main class="admin"><div>${criarNavegacaoAdmin(active)}<section class="admin-main"><span class="eyebrow">ADMIN</span><h1>${title}</h1>${content}</section></div></main>`;

// Traduz o status atual do pedido na próxima ação operacional possível.
const obterProximaAcaoPedido = (item) =>
  ({
    EM_ABERTO: "Iniciar processamento",
    EM_PROCESSAMENTO: "Confirmar pagamento",
    PAGAMENTO_REALIZADO: "Despachar",
  })[item.status];
const criarAcoesPedidoAdmin = (item) => {
  // Monta somente botões válidos para o estado atual do pedido.
  const next = obterProximaAcaoPedido(item);
  const progress = next
    ? `<button data-progress-order="${escaparHtml(item.id)}">${next}</button>`
    : "";
  const reject =
    item.status === "EM_PROCESSAMENTO"
      ? `<button class="link-danger" data-reject-payment="${escaparHtml(item.id)}">Recusar pagamento</button>`
      : "";
  return progress || reject
    ? `<div class="table-actions">${progress}${reject}</div>`
    : "—";
};

const CORES_ANALYTICS = ["#b85b78", "#c58a42", "#8f6fa8", "#5e8399", "#bf6f54", "#78966d", "#b07b93"];

function criarDashboard(query) {
  // O dashboard lê filtros da URL e cai em valores padrão se houver erro.
  const selectedFromQuery = query
    .getAll("category")
    .filter((category) => analyticsCategories.includes(category));
  const selectedCategories = selectedFromQuery.length
    ? selectedFromQuery
    : analyticsCategories;
  const requestedStart = query.get("start") || "";
  const requestedEnd = query.get("end") || "";
  let analysis;
  let validationError = "";
  try {
    analysis = salesAnalysis({
      startDate: requestedStart,
      endDate: requestedEnd,
      categories: selectedCategories,
    });
  } catch (error) {
    validationError = error.message;
    analysis = salesAnalysis({ categories: selectedCategories });
  }
  const values = analysis.series.flatMap((series) => series.values);
  const max = Math.max(...values, 1);

  // Converte índice e valor de venda em coordenadas do SVG.
  const x = (index) => 30 + (index * 350) / Math.max(analysis.periods.length - 1, 1);
  const y = (value) => 150 - (value * 118) / max;
  const totalSales = values.reduce((sum, value) => sum + value, 0);
  const seriesSvg = analysis.series
    .map((series, seriesIndex) => {
      // Cada categoria vira uma linha com pontos e títulos acessíveis no gráfico.
      const color = CORES_ANALYTICS[seriesIndex % CORES_ANALYTICS.length];
      const points = series.values
        .map((value, index) => `${x(index)},${y(value)}`)
        .join(" ");
      const circles = series.values
        .map(
          (value, index) =>
            `<circle cx="${x(index)}" cy="${y(value)}" r="5" style="fill:${color}"><title>${escaparHtml(series.category)} · ${escaparHtml(analysis.periods[index].label)}: ${formatarMoeda(value)}</title></circle>`,
        )
        .join("");
      return `<polyline fill="none" stroke="${color}" stroke-width="3" points="${points}"/>${circles}`;
    })
    .join("");
  const validOrders = state.orders.filter(
    (item) => !["EM_ABERTO", "CANCELADO"].includes(item.status),
  );
  return criarEstruturaAdmin(
    "Dashboard",
    "Dashboard",
    `<div class="metrics"><div>Receita analisada <b>${formatarMoeda(totalSales)}</b><small>categorias e período selecionados</small></div><div>Pedidos ativos <b>${validOrders.length}</b><small>processamento até entregue</small></div><div>Clientes <b>${state.customers.length}</b><small>${state.customers.filter((item) => item.active).length} ativos</small></div><div>Trocas abertas <b>${state.exchanges.filter((item) => !["TROCA_NEGADA", "TROCA_PROCESSADA"].includes(item.status)).length}</b><small>aguardando ação</small></div></div><div class="panel chart-panel"><div><span class="eyebrow">Analytics</span><h2>Análise de vendas por categoria</h2><p class="muted">Filtre o período e compare uma ou mais categorias de produtos de beleza.</p></div><form id="analytics-filter" class="analytics-filter"><label>Data inicial<input class="form-control" type="date" name="start" min="${analysis.minDate}" max="${analysis.maxDate}" value="${escaparHtml(requestedStart || analysis.startDate)}" required></label><label>Data final<input class="form-control" type="date" name="end" min="${analysis.minDate}" max="${analysis.maxDate}" value="${escaparHtml(requestedEnd || analysis.endDate)}" required></label><fieldset><legend>Categorias para comparação</legend><div class="analytics-category-selector">${analyticsCategories.map((category) => `<label><input type="checkbox" name="category" value="${escaparHtml(category)}" ${selectedCategories.includes(category) ? "checked" : ""}> ${escaparHtml(category)}</label>`).join("")}</div></fieldset><div class="analytics-actions"><button class="btn aura-btn">Aplicar filtros</button><button type="button" id="analytics-export" class="btn btn-outline-secondary">Exportar dados</button></div><p id="analytics-filter-feedback" class="text-destructive" ${validationError ? "" : "hidden"}>${escaparHtml(validationError)}</p></form><svg class="sales-chart" viewBox="0 0 420 190" role="img" aria-label="Evolução de vendas por categoria"><line x1="20" y1="155" x2="400" y2="155"/>${seriesSvg}${analysis.periods.map(({ label }, index) => `<text x="${x(index)}" y="178" text-anchor="middle">${analysis.periods.length > 12 && index % 2 ? "" : escaparHtml(label.slice(0, 3))}</text>`).join("")}</svg><div class="chart-legend">${analysis.series.map((series, index) => `<span style="--legend-color:${CORES_ANALYTICS[index % CORES_ANALYTICS.length]}">● ${escaparHtml(series.category)}</span>`).join("")}</div></div>`,
  );
}

const criarFormularioClienteAdmin = (item = null) => {
  // O mesmo formulário serve para criar e editar clientes na área administrativa.
  const criando = !item;
  const selecionar = (current, value) => (current === value ? "selected" : "");
  return `<form id="admin-customer-form" class="panel form-stack mt-3">
    <input type="hidden" name="customerId" value="${escaparHtml(item?.id || "")}">
    <div class="section-heading"><div><h2>${criando ? "Novo cliente" : "Editar cliente"}</h2><p class="muted small">${criando ? "O cadastro cria uma conta ativa com um endereço inicial de cobrança e entrega." : "Altere os dados pessoais sem apagar endereços, cartões ou histórico."}</p></div><a href="/admin/clientes" data-link>Cancelar</a></div>
    <div class="row g-3">
      <div class="col-md-6"><label>Nome completo<input class="form-control" name="name" value="${escaparHtml(item?.name || "")}" required></label></div>
      <div class="col-md-6"><label>CPF<input class="form-control" name="cpf" value="${escaparHtml(item?.cpf || "")}" required></label></div>
      <div class="col-md-6"><label>E-mail<input class="form-control" name="email" type="email" value="${escaparHtml(item?.email || "")}" required></label></div>
      <div class="col-md-3"><label>Data de nascimento<input class="form-control" name="birthDate" type="date" value="${escaparHtml(item?.birthDate || "")}" required></label></div>
      <div class="col-md-3"><label>Gênero<select class="form-select" name="gender" required><option value="">Selecione</option>${["Feminino", "Masculino", "Não binário", "Outro", "Prefiro não informar"].map((value) => `<option ${selecionar(item?.gender, value)}>${value}</option>`).join("")}</select></label></div>
      <div class="col-md-3"><label>Tipo de telefone<select class="form-select" name="phoneType" required>${["Celular", "Residencial", "Comercial"].map((value) => `<option ${selecionar(item?.phoneType, value)}>${value}</option>`).join("")}</select></label></div>
      <div class="col-md-3"><label>DDD<input class="form-control" name="phoneDdd" inputmode="numeric" maxlength="2" value="${escaparHtml(item?.phoneDdd || "")}" required></label></div>
      <div class="col-md-6"><label>Número do telefone<input class="form-control" name="phoneNumber" value="${escaparHtml(item?.phoneNumber || "")}" required></label></div>
      ${criando ? '<div class="col-md-6"><label>Senha demonstrativa<input class="form-control" name="password" type="password" minlength="6" required></label></div>' : ""}
    </div>
    ${criando ? `<hr><h2>Endereço residencial inicial</h2>${criarCamposEndereco({}, "addressLabel", true)}` : ""}
    <button class="btn aura-btn">${criando ? "Cadastrar cliente" : "Salvar alterações"}</button>
  </form>`;
};

function criarPaginaClientesAdmin(query) {
  // Aplica filtros simples e decide se o formulário administrativo fica aberto.
  const filters = {
    name: query.get("name") || "",
    cpf: query.get("cpf") || "",
    email: query.get("email") || "",
    phone: query.get("phone") || "",
  };
  const list = filterCustomers(filters);
  const editing = customer(query.get("edit"));
  const showForm = query.get("new") === "1" || Boolean(editing);
  const form = showForm ? criarFormularioClienteAdmin(editing || null) : "";
  return criarEstruturaAdmin(
    "Clientes",
    "Clientes",
    `<div class="toolbar"><p class="muted">Cadastre clientes e preserve o histórico ao inativá-los.</p><a href="${showForm ? "/admin/clientes" : "/admin/clientes?new=1"}" data-link class="btn ${showForm ? "btn-outline-secondary" : "aura-btn"}">${showForm ? "Fechar formulário" : "Cadastrar novo cliente"}</a></div>${form}<form id="admin-customer-filter" class="panel filter-bar mt-3"><input class="form-control" name="name" value="${escaparHtml(filters.name)}" placeholder="Nome"><input class="form-control" name="cpf" value="${escaparHtml(filters.cpf)}" placeholder="CPF"><input class="form-control" name="email" value="${escaparHtml(filters.email)}" placeholder="E-mail"><input class="form-control" name="phone" value="${escaparHtml(filters.phone)}" placeholder="Telefone"><button class="btn aura-btn">Filtrar</button></form><div class="panel table-responsive mt-3"><table><thead><tr><th>Cliente</th><th>CPF</th><th>Contato</th><th>Ranking</th><th>Situação</th><th>Ações</th></tr></thead><tbody>${list.map((item) => `<tr><td><b>${escaparHtml(item.name)}</b></td><td>${escaparHtml(item.cpf)}</td><td>${escaparHtml(item.email)}<small>${escaparHtml(item.phone)}</small></td><td><b>${customerRanking(item.id)} / 5</b></td><td>${status(item.active ? "ATIVO" : "INATIVO")}</td><td><div class="table-actions"><a href="/admin/clientes/${item.id}" data-link>Consultar</a><a href="/admin/clientes?edit=${encodeURIComponent(item.id)}" data-link>Editar</a>${item.active ? `<button class="link-danger" data-admin-inactivate-customer="${item.id}">Inativar</button>` : ""}</div></td></tr>`).join("") || '<tr><td colspan="6">Nenhum cliente encontrado.</td></tr>'}</tbody></table></div>`,
  );
}

function criarDetalheClienteAdmin(id) {
  // Visão administrativa consolida cadastro, transações, endereços, cartões e cupons.
  const item = customer(id);
  if (!item) return notFound(true);
  return criarEstruturaAdmin(
    "Clientes",
    item.name,
    `<div class="toolbar"><a href="/admin/clientes" data-link>← Voltar para clientes</a><div class="table-actions"><a href="/admin/clientes?edit=${encodeURIComponent(item.id)}" data-link>Editar cliente</a>${item.active ? `<button class="link-danger" data-admin-inactivate-customer="${item.id}">Inativar cliente</button>` : ""}</div></div><div class="row g-4 mt-1"><section class="col-lg-7"><div class="panel"><div class="section-heading"><h2>Cadastro</h2><div><span class="muted small">Ranking</span><h2>${customerRanking(item.id)} / 5</h2></div></div><p><b>CPF:</b> ${escaparHtml(item.cpf)}<br><b>Data de nascimento:</b> ${escaparHtml(item.birthDate)}<br><b>Gênero:</b> ${escaparHtml(item.gender)}<br><b>E-mail:</b> ${escaparHtml(item.email)}<br><b>Telefone:</b> ${escaparHtml(item.phone)}<br><b>Situação:</b> ${status(item.active ? "ATIVO" : "INATIVO")}</p></div><div class="panel mt-3"><h2>Transações</h2>${
      ordersForCustomer(item.id)
        .map(
          (value) =>
            `<p class="payment-line"><a href="/admin/pedidos/${encodeURIComponent(value.id)}" data-link>${escaparHtml(value.id)}</a><span>${status(value.status)} · ${formatarMoeda(value.total)}</span></p>`,
        )
        .join("") || "<p>Nenhum pedido.</p>"
    }</div></section><aside class="col-lg-5"><div class="panel"><h3>Endereços</h3>${item.addresses.map((value) => `<p><b>${escaparHtml(value.label)} · ${escaparHtml(descreverFinalidades(value))}</b><br>${formatarEnderecoCompleto(value)}<br>CEP ${escaparHtml(value.cep)} · ${escaparHtml(value.country)}</p>`).join("") || "<p>Nenhum.</p>"}<h3>Cartões mascarados</h3>${item.cards.map((value) => `<p>${escaparHtml(value.brand || value.label)} final ${escaparHtml(value.last4)}<br><small>${escaparHtml(value.holder || "")}</small></p>`).join("") || "<p>Nenhum.</p>"}<h3>Cupons de troca</h3>${
      customerCoupons(item.id)
        .filter((value) => value.type === "TROCA")
        .map(
          (value) =>
            `<p>${escaparHtml(value.code)} · ${formatarMoeda(value.balance)}</p>`,
        )
        .join("") || "<p>Nenhum.</p>"
    }</div></aside></div>`,
  );
}

function criarPaginaPedidosAdmin(query) {
  // Lista pedidos com filtros administrativos por número, cliente e status.
  const filters = {
    id: query.get("id") || "",
    customerId: query.get("customerId") || "",
    status: query.get("status") || "",
  };
  const list = filterOrders(filters);
  const statuses = [
    "EM_ABERTO",
    "EM_PROCESSAMENTO",
    "PAGAMENTO_REALIZADO",
    "EM_TRANSITO",
    "ENTREGUE",
    "CANCELADO",
  ];
  return criarEstruturaAdmin(
    "Pedidos",
    "Pedidos",
    `<form id="admin-order-filter" class="panel filter-bar"><input class="form-control" name="id" value="${escaparHtml(filters.id)}" placeholder="Número"><select class="form-select" name="customerId"><option value="">Todos os clientes</option>${customers()
      .map(
        (item) =>
          `<option value="${item.id}" ${filters.customerId === item.id ? "selected" : ""}>${escaparHtml(item.name)}</option>`,
      )
      .join(
        "",
      )}</select><select class="form-select" name="status"><option value="">Todos os status</option>${statuses.map((value) => `<option value="${value}" ${filters.status === value ? "selected" : ""}>${exibirStatus(value)}</option>`).join("")}</select><button class="btn aura-btn">Filtrar</button></form><div class="panel table-responsive mt-3"><table><thead><tr><th>Pedido</th><th>Cliente</th><th>Total</th><th>Status</th><th>Ações válidas</th></tr></thead><tbody>${list.map((item) => `<tr><td><a href="/admin/pedidos/${encodeURIComponent(item.id)}" data-link>${escaparHtml(item.id)}</a></td><td>${escaparHtml(customer(item.customerId)?.name || item.customerLabel || "Convidado")}</td><td>${formatarMoeda(item.total)}</td><td>${status(item.status)}${item.cancelReason ? "<small>Pagamento recusado</small>" : ""}</td><td>${criarAcoesPedidoAdmin(item)}</td></tr>`).join("")}</tbody></table></div>`,
  );
}

function criarDetalhePedidoAdmin(id) {
  // Detalhe administrativo inclui pagamento, histórico e ações de mudança de status.
  const item = order(id);
  if (!item) return notFound(true);
  return criarEstruturaAdmin(
    "Pedidos",
    `Pedido ${escaparHtml(item.id)}`,
    `<div class="row g-4"><section class="col-lg-8"><div class="panel"><div class="section-heading"><h2>Dados do pedido</h2>${status(item.status)}</div><p><b>Cliente:</b> ${item.customerId ? `<a href="/admin/clientes/${item.customerId}" data-link>${escaparHtml(customer(item.customerId)?.name)}</a>` : "Convidado"}${item.cancelReason ? "<br><b>Motivo:</b> pagamento recusado pela operadora" : ""}</p>${item.items.map((line) => `<p class="payment-line"><span>${escaparHtml(product(line.productId)?.name)} × ${line.quantity}</span><b>${formatarMoeda(line.unitPrice * line.quantity)}</b></p>`).join("")}<hr><p class="payment-line"><span>Total</span><b>${formatarMoeda(item.total)}</b></p>${criarAcoesPedidoAdmin(item)}</div><div class="panel mt-3"><h2>Composição do pagamento</h2>${item.payments.map((payment) => `<p class="payment-line"><span>${escaparHtml(payment.label)}</span><b>${formatarMoeda(payment.amount)}</b></p>`).join("")}</div></section><aside class="col-lg-4"><div class="panel"><h3>Histórico</h3>${criarLinhaDoTempo(item.history)}</div></aside></div>`,
  );
}

function criarPaginaTrocasAdmin() {
  // Para cada status de troca, exibe apenas a ação administrativa compatível.
  return criarEstruturaAdmin(
    "Trocas",
    "Trocas",
    `<div class="panel table-responsive"><table><thead><tr><th>Troca</th><th>Cliente / item</th><th>Status</th><th>Ação válida</th></tr></thead><tbody>${state.exchanges.map((item) => `<tr><td><b>${escaparHtml(item.id)}</b><small>Pedido ${escaparHtml(item.orderId)}</small></td><td>${escaparHtml(customer(item.customerId)?.name)}<small>${escaparHtml(product(item.productId)?.name)} · ${escaparHtml(item.reason)}</small></td><td>${status(item.status)}</td><td>${item.status === "TROCA_SOLICITADA" ? `<div class="table-actions"><button data-accept-exchange="${item.id}">Aceitar</button><button data-deny-exchange="${item.id}">Negar</button></div>` : item.status === "ITEM_ENVIADO" ? `<form class="receive-exchange-form" data-exchange-id="${item.id}"><label><input type="checkbox" name="restock"> Retorna ao estoque</label><button>Confirmar recebimento</button></form>` : item.status === "ITEM_RECEBIDO" ? `<button data-process-exchange="${item.id}">Processar e gerar cupom</button>` : item.status === "TROCA_ACEITA" ? "Aguardando despacho do cliente" : item.status === "TROCA_PROCESSADA" ? `Cupom ${escaparHtml(state.coupons.find((coupon) => coupon.id === item.generatedCouponId)?.code || "")}` : "Encerrada"}</td></tr>`).join("")}</tbody></table></div>`,
  );
}

function criarPaginaProdutosAdmin(query = new URLSearchParams()) {
  // Query string controla abertura do formulário e produto em edição.
  const editing = product(query.get("edit"));
  const formOpen = query.get("new") === "1" || Boolean(editing);
  const value = (key, fallback = "") => escaparHtml(editing?.[key] ?? fallback);
  const imageUrl = /^https?:\/\//i.test(editing?.image || "")
    ? editing.image
    : "";

  // Formulário e tabela ficam na mesma página para agilizar o cadastro demonstrativo.
  const form = `<div class="toolbar"><p class="muted">Cadastre, edite e controle quais itens aparecem no catálogo.</p><a href="${formOpen ? "/admin/produtos" : "/admin/produtos?new=1"}" data-link class="btn ${formOpen ? "btn-outline-secondary" : "aura-btn"}">${formOpen ? "Fechar formulário" : "Cadastrar novo produto"}</a></div>
    <form id="admin-product-form" class="panel form-stack mt-3 ${formOpen ? "" : "hidden-section"}">
      <input type="hidden" name="productId" value="${editing?.id || ""}">
      <div class="section-heading"><div><h2>${editing ? "Editar produto" : "Novo produto"}</h2><p class="muted small">${editing ? "As alterações preservam o identificador e o histórico do produto." : "Preencha os dados comerciais e de apresentação."}</p></div>${editing ? '<a href="/admin/produtos" data-link>Cancelar edição</a>' : ""}</div>
      <div class="row g-3">
        <div class="col-md-3"><label>Código / SKU<input class="form-control" name="code" value="${value("code")}" placeholder="AUR-009" required></label></div>
        <div class="col-md-5"><label>Nome do produto<input class="form-control" name="name" value="${value("name")}" required></label></div>
        <div class="col-md-4"><label>Marca<input class="form-control" name="brand" value="${value("brand")}" required></label></div>
        <div class="col-md-8"><label>Categorias<input class="form-control" name="categories" value="${escaparHtml(editing?.categories?.join(", ") || "")}" placeholder="Maquiagem, Face" required><small>Separe múltiplas categorias por vírgula.</small></label></div>
        <div class="col-md-4"><label>Volume / conteúdo<input class="form-control" name="volume" value="${value("volume")}" placeholder="30 ml"></label></div>
        <div class="col-md-4"><label>Preço de venda<input class="form-control" name="price" type="number" min="0.01" step="0.01" value="${value("price")}" required></label></div>
        <div class="col-md-4"><label>Custo<input class="form-control" name="cost" type="number" min="0" step="0.01" value="${value("cost")}" required></label></div>
        <div class="col-md-4"><label>Estoque<input class="form-control" name="stock" type="number" min="0" step="1" value="${value("stock")}" required></label></div>
        <div class="col-12"><label>Descrição<textarea class="form-control" name="description" rows="3" required>${value("description")}</textarea></label></div>
        <div class="col-md-4"><label>Tonalidade<input class="form-control" name="tone" value="${value("tone")}" placeholder="Ex.: Rosa pétala"></label></div>
        <div class="col-md-4"><label>Acabamento<input class="form-control" name="finish" value="${value("finish")}" placeholder="Ex.: Natural"></label></div>
        <div class="col-md-4"><label>Tipo de pele<input class="form-control" name="skin" value="${value("skin")}" placeholder="Ex.: Todos os tipos"></label></div>
        <div class="col-12"><label>Ingredientes em destaque<textarea class="form-control" name="ingredients" rows="2">${value("ingredients")}</textarea></label></div>
        <div class="col-md-5"><label>URL da imagem<input class="form-control" name="imageUrl" type="url" value="${escaparHtml(imageUrl)}" placeholder="https://..."></label></div>
        <div class="col-md-5"><label>Ou selecionar imagem<input class="form-control" name="imageFile" type="file" accept="image/png,image/jpeg,image/webp,image/gif"><small>Até 1,5 MB para o protótipo local.</small></label></div>
        <div class="col-md-2"><label>Cor de apoio<input class="form-control form-control-color" name="art" type="color" value="${value("art", "#b65d78")}"></label></div>
      </div>
      <div id="product-image-preview" class="product-image-preview"><span ${editing?.image ? "hidden" : ""}>A prévia da imagem aparecerá aqui.</span><img src="${escaparHtml(editing?.image || "")}" alt="Prévia do produto" ${editing?.image ? "" : "hidden"}></div>
      <label class="form-check"><input class="form-check-input" type="checkbox" name="visible" ${editing ? (editing.visible ? "checked" : "") : "checked"}> Publicar no catálogo</label>
      <button class="btn aura-btn" type="submit">${editing ? "Salvar alterações" : "Cadastrar produto"}</button>
    </form>`;
  const table = `<div class="panel table-responsive mt-3"><div class="section-heading"><div><h2>Produtos cadastrados</h2><p class="muted">Produtos com histórico são ocultados em vez de excluídos.</p></div><b>${state.products.length} itens</b></div><table><thead><tr><th>Produto</th><th>Categoria</th><th>Preço</th><th>Estoque</th><th>Visibilidade</th><th>Ações</th></tr></thead><tbody>${state.products.map((item) => `<tr><td><b>${escaparHtml(item.name)}</b><small>${escaparHtml(item.code)} · ${escaparHtml(item.brand)}</small></td><td>${escaparHtml(item.categories.join(", "))}</td><td>${formatarMoeda(item.price)}</td><td>${item.stock}</td><td>${status(item.visible ? "VISIVEL" : "OCULTO")}</td><td><div class="table-actions"><a href="/admin/produtos?edit=${item.id}" data-link>Editar</a><button data-toggle-product="${item.id}">${item.visible ? "Ocultar" : "Reativar"}</button></div></td></tr>`).join("")}</tbody></table></div>`;
  return criarEstruturaAdmin("Produtos", "Produtos", form + table);
}

function criarPaginaCuponsAdmin(query = new URLSearchParams()) {
  // Cupons de troca são listados, mas somente promocionais podem ser editados.
  const editing =
    state.coupons.find(
      (item) => item.id === query.get("edit") && item.type === "PROMOCIONAL",
    ) || null;
  const form = `<div class="toolbar"><p class="muted">Cupons promocionais são administrados manualmente; créditos de troca são automáticos.</p><button type="button" class="btn ${editing ? "btn-outline-secondary" : "aura-btn"}" data-toggle-coupon-form>${editing ? "Fechar edição" : "Cadastrar cupom promocional"}</button></div><form id="admin-coupon-form" class="panel form-stack ${editing ? "" : "hidden-section"}"><input type="hidden" name="couponId" value="${escaparHtml(editing?.id || "")}"><div class="section-heading"><div><h2>${editing ? "Alterar cupom promocional" : "Novo cupom promocional"}</h2><p class="muted">O código será convertido para letras maiúsculas.</p></div>${editing ? '<a href="/admin/cupons" data-link>Cancelar edição</a>' : ""}</div><div class="row g-3"><div class="col-md-4"><label>Código<input class="form-control" name="code" value="${escaparHtml(editing?.code || "")}" placeholder="AURA15" required></label></div><div class="col-md-4"><label>Tipo de desconto<select class="form-select" name="mode" required><option value="PERCENTUAL" ${editing?.mode === "PERCENTUAL" ? "selected" : ""}>Percentual (%)</option><option value="FIXO" ${editing?.mode === "FIXO" ? "selected" : ""}>Valor fixo (R$)</option></select></label></div><div class="col-md-4"><label>Valor<input class="form-control" name="value" type="number" min="0.01" step="0.01" value="${escaparHtml(editing?.value || "")}" required></label></div><div class="col-md-4"><label>Validade<input class="form-control" name="validUntil" type="date" value="${escaparHtml(editing?.validUntil || "")}" required></label></div><div class="col-md-4"><label>Compra mínima<input class="form-control" name="minPurchase" type="number" min="0" step="0.01" value="${escaparHtml(editing?.minPurchase || 0)}"></label></div><div class="col-md-4"><label>Limite total de usos <small>(opcional)</small><input class="form-control" name="limit" type="number" min="1" step="1" value="${escaparHtml(editing?.limit ?? "")}"></label></div></div><button class="btn aura-btn">${editing ? "Salvar alterações" : "Cadastrar cupom"}</button></form>`;
  const rows = state.coupons
    .map((item) => {
      // Valor exibido muda conforme o tipo do cupom: desconto ou saldo.
      const value =
        item.type === "TROCA"
          ? formatarMoeda(item.balance)
          : item.mode === "PERCENTUAL"
            ? `${item.value}%`
            : formatarMoeda(item.value);
      const origin =
        item.type === "TROCA"
          ? `Troca ${escaparHtml(item.sourceExchangeId || "não identificada")}`
          : `${item.uses || 0} uso(s)${item.limit == null ? "" : " de " + item.limit}`;
      const actions =
        item.type === "TROCA"
          ? '<span class="muted small">Gerado automaticamente<br>Não editável</span>'
          : `<div class="table-actions"><a href="/admin/cupons?edit=${encodeURIComponent(item.id)}" data-link>Alterar</a>${item.active ? `<button class="link-danger" data-inactivate-promo="${item.id}">Inativar</button>` : ""}</div>`;
      return `<tr><td><b>${escaparHtml(item.code)}</b><small>${origin}</small></td><td>${escaparHtml(item.type)}</td><td>${item.customerId ? escaparHtml(customer(item.customerId)?.name) : "Todos os clientes"}</td><td>${value}</td><td>${escaparHtml(item.validUntil)}</td><td>${status(item.active ? "ATIVO" : "INATIVO")}</td><td>${actions}</td></tr>`;
    })
    .join("");
  return criarEstruturaAdmin(
    "Cupons",
    "Cupons",
    form +
      `<div class="panel table-responsive mt-3"><table><thead><tr><th>Código / origem</th><th>Tipo</th><th>Cliente</th><th>Valor/saldo</th><th>Validade</th><th>Situação</th><th>Ações</th></tr></thead><tbody>${rows}</tbody></table></div>`,
  );
}

function criarPaginaConfiguracoesAdmin() {
  // Tela simples para voltar a massa local ao estado inicial.
  return criarEstruturaAdmin(
    "Configurações",
    "Configurações",
    `<div class="panel"><h2>Massa de demonstração</h2><p>Restaura produtos, clientes, pedidos, trocas, cupons, endereços, cartões e gráfico para os valores iniciais.</p><button class="btn btn-outline-danger" data-reset-demo>Restaurar demonstração</button><hr><p>Registros de auditoria locais: <b>${state.audit.length}</b></p><p class="muted">Ator administrativo: ADMIN.</p></div>`,
  );
}

export function adminPage(section, query, id = null) {
  // Roteador interno da área administrativa.
  if (section === "Dashboard") return criarDashboard(query);
  if (section === "Clientes")
    return id ? criarDetalheClienteAdmin(id) : criarPaginaClientesAdmin(query);
  if (section === "Pedidos")
    return id ? criarDetalhePedidoAdmin(id) : criarPaginaPedidosAdmin(query);
  if (section === "Trocas") return criarPaginaTrocasAdmin();
  if (section === "Produtos") return criarPaginaProdutosAdmin(query);
  if (section === "Cupons") return criarPaginaCuponsAdmin(query);
  return criarPaginaConfiguracoesAdmin();
}

export function notFound(admin = false) {
  // Renderiza a página 404 no layout correto: loja ou administração.
  const content =
    '<div class="panel"><h1>Página não encontrada.</h1><a href="/" data-link>Voltar ao início</a></div>';
  return admin
    ? criarEstruturaAdmin("", "Não encontrada", content)
    : criarLayoutCliente(
        `<main class="page"><div class="container aura-container page-top">${content}</div></main>`,
      );
}
