/**
 * Estado local e regras de negócio do protótipo.
 * A fachada permanece neste arquivo para preservar os imports já usados pelas telas e pelos testes.
 */
import {
  DADOS_INICIAIS,
  CATEGORIAS_ANALYTICS,
  HISTORICO_ANALYTICS,
  HISTORICO_ANALYTICS_CATEGORIAS,
  VERSAO_SCHEMA,
} from "./dados-mock.js";
import {
  agoraEmIso,
  arredondarMoeda,
  clonarDados,
  criarIdentificador,
  dataAtualEmPtBr,
} from "./utilitarios.js";

const CHAVE_DEMONSTRACAO = "aura-lab-v3";
const MINUTOS_RESERVA = 20;

export {
  CATEGORIAS_ANALYTICS as analyticsCategories,
  HISTORICO_ANALYTICS as analyticsHistory,
};

export function salesAnalysis({ startDate, endDate, categories = [] } = {}) {
  // Define automaticamente o menor e o maior período possível a partir do histórico.
  const dates = HISTORICO_ANALYTICS_CATEGORIAS.map((item) => item.date);
  const minDate = dates[0];
  const latestRecordDate = dates[dates.length - 1];
  const [latestYear, latestMonth] = latestRecordDate.split("-").map(Number);
  const maxDate = new Date(Date.UTC(latestYear, latestMonth, 0))
    .toISOString()
    .slice(0, 10);
  const initialDate = startDate || minDate;
  const finalDate = endDate || maxDate;
  if (initialDate > finalDate)
    throw Error("A data final não pode ser anterior à data inicial.");

  // Remove categorias desconhecidas para impedir filtros inválidos na análise.
  const selectedCategories = [...new Set(categories)].filter((category) =>
    CATEGORIAS_ANALYTICS.includes(category),
  );
  const selected = selectedCategories.length
    ? selectedCategories
    : [...CATEGORIAS_ANALYTICS];
  const records = HISTORICO_ANALYTICS_CATEGORIAS.filter(
    (item) => item.date >= initialDate && item.date <= finalDate,
  );
  const periods = [...new Map(records.map((item) => [item.date, item.period]))]
    .map(([date, label]) => ({ date, label }));

  // Cada série representa uma categoria, com um valor para cada período do gráfico.
  const series = selected.map((category) => ({
    category,
    values: periods.map(({ date }) =>
      records
        .filter((item) => item.category === category && item.date === date)
        .reduce((sum, item) => sum + item.value, 0),
    ),
  }));
  return { minDate, maxDate, startDate: initialDate, endDate: finalDate, periods, series };
}

const load = () => {
  // Tenta reaproveitar o estado salvo no navegador, mas só se a versão for compatível.
  const fresh = clonarDados(DADOS_INICIAIS);
  try {
    const saved = JSON.parse(localStorage.getItem(CHAVE_DEMONSTRACAO) || "null");
    if (saved?.version === VERSAO_SCHEMA) return saved;
  } catch {
    // Estado inválido é substituído pela massa de demonstração.
  }
  return fresh;
};

export const state = load();

// Persiste todo o estado local para manter a demonstração após recarregar a página.
export const persist = () =>
  localStorage.setItem(
    CHAVE_DEMONSTRACAO,
    JSON.stringify({ ...state, version: VERSAO_SCHEMA }),
  );

// Restaura uma cópia nova dos mocks para que a apresentação sempre possa voltar ao roteiro inicial.
export function resetDemo() {
  const fresh = clonarDados(DADOS_INICIAIS);
  Object.keys(state).forEach((key) => delete state[key]);
  Object.assign(state, fresh);
  persist();
}
export function audit(operation, changes, actor = "SISTEMA") {
  // Guarda as operações mais recentes para inspeção administrativa do protótipo.
  state.audit.unshift({
    id: criarIdentificador("audit"),
    at: agoraEmIso(),
    actor,
    operation,
    changes,
  });
  state.audit = state.audit.slice(0, 100);
  persist();
}

export const customer = (id) => state.customers.find((item) => item.id === id);
export const currentCustomer = () => customer(state.currentCustomerId);
export const customers = () => state.customers;
export const cardBrands = ["Visa", "Mastercard", "Elo", "American Express"];
export function customerRanking(customerId) {
  // Ranking simples: compras efetivadas aumentam a nota, limitada a 5.
  const effectiveStatuses = ["PAGAMENTO_REALIZADO", "EM_TRANSITO", "ENTREGUE"];
  const purchaseTotal = state.orders
    .filter(
      (item) =>
        item.customerId === customerId &&
        effectiveStatuses.includes(item.status),
    )
    .reduce((sum, item) => sum + Number(item.total || 0), 0);
  return Math.min(5, 1 + Math.floor(purchaseTotal / 200));
}
function createCustomerRecord(
  data,
  { actor = "CLIENTE", makeCurrent = true } = {},
) {
  // O mesmo cadastro atende a tela do cliente e a criação feita pelo administrador.
  const customerRequired = [
    "name",
    "cpf",
    "email",
    "gender",
    "birthDate",
    "phoneType",
    "phoneDdd",
    "phoneNumber",
    "password",
  ];
  const addressRequired = [
    "addressLabel",
    "residenceType",
    "streetType",
    "street",
    "number",
    "district",
    "cep",
    "city",
    "state",
    "country",
  ];
  if (
    customerRequired
      .concat(addressRequired)
      .some((key) => !String(data[key] || "").trim())
  )
    throw Error(
      "Preencha todos os dados obrigatórios do cliente e do endereço residencial.",
    );
  if (
    state.customers.some(
      (item) =>
        item.email.toLowerCase() === data.email.trim().toLowerCase() ||
        item.cpf === data.cpf.trim(),
    )
  )
    throw Error("Já existe cliente com este CPF ou e-mail.");

  // O primeiro endereço nasce como cobrança e entrega para liberar o checkout.
  const phone =
    data.phoneType.trim() +
    " (" +
    data.phoneDdd.trim() +
    ") " +
    data.phoneNumber.trim();
  const initialAddress = {
    id: criarIdentificador("addr"),
    label: data.addressLabel.trim(),
    purposes: ["COBRANCA", "ENTREGA"],
    residenceType: data.residenceType.trim(),
    streetType: data.streetType.trim(),
    street: data.street.trim(),
    number: data.number.trim(),
    district: data.district.trim(),
    cep: data.cep.trim(),
    city: data.city.trim(),
    state: data.state.trim().toUpperCase(),
    country: data.country.trim(),
    observations: String(data.observations || "").trim(),
    preferred: true,
    active: true,
  };
  const item = {
    id: criarIdentificador("cli"),
    name: data.name.trim(),
    cpf: data.cpf.trim(),
    birthDate: data.birthDate,
    gender: data.gender,
    email: data.email.trim(),
    phoneType: data.phoneType.trim(),
    phoneDdd: data.phoneDdd.trim(),
    phoneNumber: data.phoneNumber.trim(),
    phone,
    passwordChangedAt: agoraEmIso(),
    active: true,
    addresses: [initialAddress],
    cards: [],
  };
  state.customers.push(item);
  if (makeCurrent) state.currentCustomerId = item.id;
  audit("CADASTRAR_CLIENTE", { customerId: item.id }, actor);
  return item;
}
export function createCustomerByAdmin(data) {
  return createCustomerRecord(data, { actor: "ADMIN", makeCurrent: false });
}
export function createCustomer(data) {
  return createCustomerRecord(data);
}
export function changePassword(id, data) {
  // Senha é apenas demonstrativa; o protótipo não implementa autenticação real.
  const item = customer(id);
  if (!item) throw Error("Cliente não encontrado.");
  if (
    !String(data.currentPassword || "").trim() ||
    !String(data.newPassword || "").trim()
  )
    throw Error("Informe a senha atual e a nova senha.");
  if (String(data.newPassword).length < 6)
    throw Error("A nova senha deve ter ao menos 6 caracteres.");
  if (data.newPassword !== data.confirmPassword)
    throw Error("A confirmação da nova senha não confere.");
  item.passwordChangedAt = agoraEmIso();
  audit("ALTERAR_SENHA", { customerId: item.id }, "CLIENTE");
  return item;
}
export function updateCustomer(id, data, actor = "CLIENTE") {
  // Atualiza dados pessoais sem apagar cartões, endereços, pedidos ou histórico.
  const item = customer(id);
  if (!item) throw Error("Cliente não encontrado.");
  if (
    [
      "name",
      "cpf",
      "email",
      "gender",
      "birthDate",
      "phoneType",
      "phoneDdd",
      "phoneNumber",
    ].some((key) => !String(data[key] || "").trim())
  )
    throw Error("Preencha todos os dados obrigatórios do cliente.");
  if (
    state.customers.some(
      (other) =>
        other.id !== id &&
        (other.email.toLowerCase() === data.email.trim().toLowerCase() ||
          other.cpf === data.cpf.trim()),
    )
  )
    throw Error("Já existe outro cliente com este CPF ou e-mail.");
  const phone =
    data.phoneType.trim() +
    " (" +
    data.phoneDdd.trim() +
    ") " +
    data.phoneNumber.trim();
  Object.assign(item, {
    name: data.name.trim(),
    cpf: data.cpf.trim(),
    birthDate: data.birthDate,
    gender: data.gender,
    email: data.email.trim(),
    phoneType: data.phoneType.trim(),
    phoneDdd: data.phoneDdd.trim(),
    phoneNumber: data.phoneNumber.trim(),
    phone,
  });
  audit("ALTERAR_CLIENTE", { customerId: id }, actor);
}
export function inactivateCustomer(id, actor = null) {
  // Inativar preserva histórico e vínculos, apenas bloqueia novas compras do cliente.
  const item = customer(id);
  if (!item) throw Error("Cliente não encontrado.");
  if (!item.active) throw Error("Este cliente já está inativo.");
  item.active = false;
  audit(
    "INATIVAR_CLIENTE",
    { customerId: id },
    actor || (id === state.currentCustomerId ? "CLIENTE" : "ADMIN"),
  );
}
const normalizePreferred = (items, preferredId) =>
  items.forEach((item) => {
    item.preferred = item.id === preferredId && item.active !== false;
  });

// Ajuda a validar se ainda existe endereço ativo para uma finalidade específica.
const activeAddressesByPurpose = (owner, purpose, exceptId = null) =>
  owner.addresses.filter(
    (item) =>
      item.active && item.id !== exceptId && item.purposes?.includes(purpose),
  );
export function saveAddress(customerId, data, id = null) {
  // Cria ou edita endereço mantendo as regras de cobrança, entrega e preferência.
  const owner = customer(customerId);
  if (!owner) throw Error("Cliente não encontrado.");
  const required = [
    "label",
    "residenceType",
    "streetType",
    "street",
    "number",
    "district",
    "cep",
    "city",
    "state",
    "country",
  ];
  if (required.some((key) => !String(data[key] || "").trim()))
    throw Error("Preencha todos os campos obrigatórios do endereço.");
  const purposes = [
    data.billing ? "COBRANCA" : null,
    data.delivery ? "ENTREGA" : null,
  ].filter(Boolean);
  if (!purposes.length)
    throw Error("Selecione endereço de cobrança e/ou de entrega.");

  // Antes de remover uma finalidade, garante que o cliente não ficará sem endereço válido.
  let item = owner.addresses.find((address) => address.id === id);
  if (id && !item) throw Error("Endereço não encontrado.");
  if (item?.active) {
    if (
      item.purposes?.includes("COBRANCA") &&
      !purposes.includes("COBRANCA") &&
      !activeAddressesByPurpose(owner, "COBRANCA", id).length
    )
      throw Error("O cliente deve manter ao menos um endereço de cobrança.");
    if (
      item.purposes?.includes("ENTREGA") &&
      !purposes.includes("ENTREGA") &&
      !activeAddressesByPurpose(owner, "ENTREGA", id).length
    )
      throw Error("O cliente deve manter ao menos um endereço de entrega.");
  }
  const values = Object.fromEntries(
    required
      .concat("observations")
      .map((key) => [key, String(data[key] || "").trim()]),
  );
  values.state = values.state.toUpperCase();
  values.purposes = purposes;

  // Se não existir id, cria novo endereço; caso contrário, atualiza o existente.
  if (item) Object.assign(item, values);
  else {
    item = {
      id: criarIdentificador("addr"),
      ...values,
      preferred: owner.addresses.length === 0,
      active: true,
    };
    owner.addresses.push(item);
  }
  if (!purposes.includes("ENTREGA")) item.preferred = false;
  const activeDeliveryAddresses = activeAddressesByPurpose(owner, "ENTREGA");
  if (
    purposes.includes("ENTREGA") &&
    (data.preferred || activeDeliveryAddresses.length === 1)
  )
    normalizePreferred(owner.addresses, item.id);
  else if (
    !owner.addresses.some(
      (address) =>
        address.active &&
        address.preferred &&
        address.purposes?.includes("ENTREGA"),
    ) &&
    activeDeliveryAddresses.length
  )
    normalizePreferred(owner.addresses, activeDeliveryAddresses[0].id);
  audit(
    id ? "ALTERAR_ENDERECO" : "CADASTRAR_ENDERECO",
    { customerId, addressId: item.id },
    "CLIENTE",
  );
  return item;
}
export function setPreferredAddress(customerId, id) {
  // Somente endereços ativos de entrega podem ser marcados como preferenciais.
  const owner = customer(customerId);
  if (
    !owner.addresses.some(
      (item) =>
        item.id === id && item.active && item.purposes?.includes("ENTREGA"),
    )
  )
    throw Error("Selecione um endereço de entrega ativo.");
  normalizePreferred(owner.addresses, id);
  audit(
    "DEFINIR_ENDERECO_PREFERENCIAL",
    { customerId, addressId: id },
    "CLIENTE",
  );
}
export function inactivateAddress(customerId, id) {
  // Endereço é inativado em vez de excluído para preservar pedidos e histórico.
  const owner = customer(customerId);
  const item = owner.addresses.find((address) => address.id === id);
  if (!item) throw Error("Endereço não encontrado.");
  if (
    item.active &&
    item.purposes?.includes("COBRANCA") &&
    !activeAddressesByPurpose(owner, "COBRANCA", id).length
  )
    throw Error("Não é possível inativar o único endereço de cobrança.");
  if (
    item.active &&
    item.purposes?.includes("ENTREGA") &&
    !activeAddressesByPurpose(owner, "ENTREGA", id).length
  )
    throw Error("Não é possível inativar o único endereço de entrega.");
  item.active = false;
  if (item.preferred) {
    const next = owner.addresses.find(
      (address) => address.active && address.purposes?.includes("ENTREGA"),
    );
    if (next) normalizePreferred(owner.addresses, next.id);
  }
  audit("INATIVAR_ENDERECO", { customerId, addressId: id }, "CLIENTE");
}
export function saveCard(customerId, data, id = null) {
  // Valida número e CVV, mas salva apenas bandeira, nome impresso e últimos 4 dígitos.
  const owner = customer(customerId);
  const brand = String(data.brand || "").trim();
  const holder = String(data.holder || "").trim();
  const number = String(data.number || "").replace(/[^0-9]/g, "");
  const securityCode = String(data.securityCode || "").replace(/[^0-9]/g, "");
  if (!cardBrands.includes(brand))
    throw Error("Selecione uma bandeira cadastrada no sistema.");
  if (
    !holder ||
    !/^[0-9]{13,19}$/.test(number) ||
    !/^[0-9]{3,4}$/.test(securityCode)
  )
    throw Error(
      "Informe número, nome impresso, bandeira e código de segurança válidos.",
    );
  let item = owner.cards.find((card) => card.id === id);
  // Número completo e código de segurança existem apenas durante esta validação.
  // O estado local recebe somente dados mascarados, como faria a futura resposta segura da API.
  const safeValues = { brand, label: brand, holder, last4: number.slice(-4) };
  if (item) Object.assign(item, safeValues);
  else {
    item = {
      id: criarIdentificador("card"),
      ...safeValues,
      preferred: owner.cards.length === 0,
      active: true,
    };
    owner.cards.push(item);
  }
  if (data.preferred || owner.cards.filter((card) => card.active).length === 1)
    normalizePreferred(owner.cards, item.id);
  audit(
    id ? "ALTERAR_CARTAO" : "CADASTRAR_CARTAO",
    { customerId, cardId: item.id, brand: item.brand, last4: item.last4 },
    "CLIENTE",
  );
  return item;
}
export function setPreferredCard(customerId, id) {
  // Garante que apenas um cartão ativo fique marcado como preferencial.
  const owner = customer(customerId);
  if (!owner.cards.some((item) => item.id === id && item.active))
    throw Error("Cartão ativo não encontrado.");
  normalizePreferred(owner.cards, id);
  audit("DEFINIR_CARTAO_PREFERENCIAL", { customerId, cardId: id }, "CLIENTE");
}
export function inactivateCard(customerId, id) {
  // Cartões também são inativados para não quebrar pedidos antigos.
  const owner = customer(customerId);
  const item = owner.cards.find((card) => card.id === id);
  if (!item) throw Error("Cartão não encontrado.");
  item.active = false;
  if (item.preferred) {
    const next = owner.cards.find((card) => card.active);
    if (next) normalizePreferred(owner.cards, next.id);
  }
  audit("INATIVAR_CARTAO", { customerId, cardId: id }, "CLIENTE");
}

export const products = () => state.products.filter((item) => item.visible);
export const product = (id) =>
  state.products.find((item) => item.id === Number(id));
export const available = (item) =>
  Math.max(0, item.stock - (state.cart[item.id] || 0));
export function expireReservation() {
  // Quando a reserva vence, limpa a sacola e registra quais produtos saíram.
  if (!state.reservation || Date.now() < state.reservation.expiresAt)
    return false;
  state.expiredItems = Object.keys(state.cart)
    .map((id) => product(id)?.name)
    .filter(Boolean);
  state.cart = {};
  state.reservation = null;
  resetCheckout();
  return state.expiredItems.length > 0;
}
export const reservationRemaining = () =>
  state.reservation
    ? Math.max(0, Math.ceil((state.reservation.expiresAt - Date.now()) / 60000))
    : 0;
export const cart = () => {
  // Ler a sacola também verifica se a reserva ainda está válida.
  expireReservation();
  return Object.entries(state.cart)
    .map(([id, quantity]) => ({ p: product(id), q: Number(quantity) }))
    .filter((line) => line.p);
};
export const cartSubtotal = () =>
  arredondarMoeda(cart().reduce((sum, line) => sum + line.p.price * line.q, 0));
export function add(id, quantity = 1) {
  // A reserva de 20 minutos é apenas conceitual nesta fase. Ela protege a jornada do protótipo,
  // mas a concorrência e a fonte de verdade do estoque pertencerão ao back-end.
  expireReservation();
  const item = product(id);
  const qty = Math.max(1, Number(quantity) || 1);
  if (!item || !item.visible || (state.cart[id] || 0) + qty > item.stock)
    return false;
  state.cart[id] = (state.cart[id] || 0) + qty;
  state.reservation = {
    expiresAt: Date.now() + MINUTOS_RESERVA * 60000,
    lastItemAt: agoraEmIso(),
  };
  state.expiredItems = [];
  persist();
  return true;
}
export function amount(id, delta) {
  // Ajusta a quantidade respeitando estoque mínimo e máximo.
  const item = product(id);
  if (!item) return;
  const next = Math.max(
    0,
    Math.min(item.stock, (state.cart[id] || 0) + Number(delta)),
  );
  if (next) state.cart[id] = next;
  else delete state.cart[id];
  state.reservation = Object.keys(state.cart).length
    ? {
        expiresAt: Date.now() + MINUTOS_RESERVA * 60000,
        lastItemAt: agoraEmIso(),
      }
    : null;
  persist();
}
export function removeCartItem(id) {
  // Remove um item específico e encerra a reserva se a sacola ficar vazia.
  delete state.cart[id];
  state.reservation = Object.keys(state.cart).length ? state.reservation : null;
  persist();
}

export function selectedAddress() {
  // Prioriza o endereço novo do checkout; se não houver, usa o endereço salvo escolhido.
  if (state.checkout.newAddress) return state.checkout.newAddress;
  return (
    currentCustomer()?.addresses.find(
      (item) =>
        item.id === state.checkout.addressId &&
        item.active &&
        item.purposes?.includes("ENTREGA"),
    ) || null
  );
}
export function shippingFor(address = selectedAddress()) {
  // Frete demonstrativo: grátis acima de R$ 199, menor para SP e maior para outros estados.
  const sub = cartSubtotal();
  if (!sub || sub >= 199) return 0;
  return address?.state?.toUpperCase() === "SP" ? 14.9 : 24.9;
}
export function setCheckoutAddress(data) {
  // Salva a escolha de entrega, podendo reutilizar endereço do perfil ou criar um temporário.
  const owner = currentCustomer();
  if (!owner) throw Error("Selecione o cliente demonstrativo para continuar.");
  if (data.addressId && data.addressId !== "new") {
    const address = owner.addresses.find(
      (item) =>
        item.id === data.addressId &&
        item.active &&
        item.purposes?.includes("ENTREGA"),
    );
    if (!address) throw Error("Selecione um endereço válido.");
    state.checkout.addressId = address.id;
    state.checkout.newAddress = null;
  } else {
    // Novo endereço pode ser usado só neste checkout ou salvo no perfil do cliente.
    const required = [
      "label",
      "residenceType",
      "streetType",
      "street",
      "number",
      "district",
      "cep",
      "city",
      "state",
      "country",
    ];
    if (required.some((key) => !String(data[key] || "").trim()))
      throw Error("Preencha o novo endereço.");
    const newAddress = {
      id: criarIdentificador("checkout-address"),
      purposes: ["ENTREGA"],
      ...Object.fromEntries(
        required
          .concat("observations")
          .map((key) => [key, String(data[key] || "").trim()]),
      ),
    };
    newAddress.state = newAddress.state.toUpperCase();
    if (data.saveAddress) {
      const saved = saveAddress(owner.id, {
        ...newAddress,
        delivery: true,
        preferred: false,
      });
      state.checkout.addressId = saved.id;
      state.checkout.newAddress = null;
    } else {
      state.checkout.addressId = null;
      state.checkout.newAddress = newAddress;
    }
  }
  persist();
}

export const promotionalCoupons = () =>
  state.coupons.filter((item) => item.type === "PROMOCIONAL");
export const exchangeCoupons = (customerId = state.currentCustomerId) =>
  state.coupons.filter(
    (item) => item.type === "TROCA" && item.customerId === customerId,
  );
const couponValid = (item) =>
  item.active &&
  new Date(`${item.validUntil}T23:59:59`) >= new Date() &&
  (item.limit == null || item.uses < item.limit) &&
  cartSubtotal() >= (item.minPurchase || 0);
export function applyPromo(code) {
  // Campo vazio remove cupom promocional; código preenchido precisa existir e estar válido.
  if (!String(code || "").trim()) {
    state.checkout.promoCouponId = null;
    persist();
    return null;
  }
  const item = promotionalCoupons().find(
    (coupon) => coupon.code === code.trim().toUpperCase(),
  );
  if (!item || !couponValid(item))
    throw Error("Cupom promocional inválido, expirado ou indisponível.");
  state.checkout.promoCouponId = item.id;
  persist();
  return item;
}
export function savePromotionalCoupon(data, id = null) {
  // Cupons promocionais são os únicos cadastrados manualmente pelo administrador.
  const code = String(data.code || "")
    .trim()
    .toUpperCase();
  const mode = String(data.mode || "")
    .trim()
    .toUpperCase();
  const value = Number(data.value);
  const minPurchase = Number(data.minPurchase || 0);
  const limit = String(data.limit || "").trim() ? Number(data.limit) : null;
  const validUntil = String(data.validUntil || "").trim();
  if (!code || !["PERCENTUAL", "FIXO"].includes(mode) || !validUntil)
    throw Error("Preencha código, tipo de desconto, valor e validade.");
  if (
    !Number.isFinite(value) ||
    value <= 0 ||
    (mode === "PERCENTUAL" && value > 100)
  )
    throw Error("Informe um valor de desconto válido.");
  if (!Number.isFinite(minPurchase) || minPurchase < 0)
    throw Error("A compra mínima não pode ser negativa.");
  if (limit !== null && (!Number.isInteger(limit) || limit < 1))
    throw Error("O limite de usos deve ser um número inteiro positivo.");
  if (
    state.coupons.some(
      (item) => item.id !== id && item.code.toUpperCase() === code,
    )
  )
    throw Error("Já existe um cupom com este código.");

  // Edição preserva o id; cadastro novo começa ativo e sem usos.
  let item = state.coupons.find((coupon) => coupon.id === id);
  if (id && (!item || item.type !== "PROMOCIONAL"))
    throw Error("Somente cupons promocionais podem ser alterados manualmente.");
  const values = {
    code,
    type: "PROMOCIONAL",
    mode,
    value: arredondarMoeda(value),
    validUntil,
    minPurchase: arredondarMoeda(minPurchase),
    limit,
    customerId: null,
  };
  if (item) Object.assign(item, values);
  else {
    item = {
      id: criarIdentificador("promo"),
      ...values,
      uses: 0,
      active: true,
    };
    state.coupons.unshift(item);
  }
  audit(
    id ? "ALTERAR_CUPOM_PROMOCIONAL" : "CADASTRAR_CUPOM_PROMOCIONAL",
    { couponId: item.id, code: item.code },
    "ADMIN",
  );
  return item;
}
export function inactivatePromotionalCoupon(id) {
  // Inativar mantém o cupom no histórico, mas impede novos usos.
  const item = state.coupons.find((coupon) => coupon.id === id);
  if (!item || item.type !== "PROMOCIONAL")
    throw Error(
      "Somente cupons promocionais podem ser inativados manualmente.",
    );
  if (!item.active) throw Error("Este cupom promocional já está inativo.");
  item.active = false;
  audit(
    "INATIVAR_CUPOM_PROMOCIONAL",
    { couponId: item.id, code: item.code },
    "ADMIN",
  );
  return item;
}
export function setCheckoutPayments(data) {
  // Recalcula cupons e cartões escolhidos para a etapa de pagamento.
  const owner = currentCustomer();
  if (!owner) throw Error("Selecione o cliente demonstrativo para continuar.");
  applyPromo(data.promoCode || "");
  const allowed = new Set(
    exchangeCoupons()
      .filter(couponValid)
      .filter((item) => item.balance > 0)
      .map((item) => item.id),
  );
  state.checkout.exchangeCouponIds = (data.exchangeCouponIds || []).filter(
    (id) => allowed.has(id),
  );

  // Valores negativos viram zero e cartões inativos são ignorados.
  const activeCards = owner.cards.filter((item) => item.active);
  state.checkout.cardAllocations = Object.fromEntries(
    activeCards
      .map((card) => [
        card.id,
        arredondarMoeda(
          Math.max(0, Number(data.cardAllocations?.[card.id] || 0)),
        ),
      ])
      .filter(([, value]) => value > 0),
  );
  persist();
}
export function checkoutTotals() {
  // Cupom promocional reduz o preço; cupom de troca entra como crédito de pagamento.
  // O restante precisa ser distribuído exatamente entre um ou mais cartões.
  const subtotal = cartSubtotal();
  const promo = state.coupons.find(
    (item) => item.id === state.checkout.promoCouponId && couponValid(item),
  );
  const discount = promo
    ? arredondarMoeda(
        promo.mode === "PERCENTUAL"
          ? (subtotal * promo.value) / 100
          : Math.min(subtotal, promo.value),
      )
    : 0;
  const shipping = shippingFor();
  const total = arredondarMoeda(subtotal - discount + shipping);
  const selectedTrades = state.checkout.exchangeCouponIds
    .map((id) => state.coupons.find((item) => item.id === id))
    .filter((item) => item && couponValid(item) && item.balance > 0);
  const exchangeAvailable = arredondarMoeda(
    selectedTrades.reduce((sum, item) => sum + item.balance, 0),
  );
  const exchangeApplied = Math.min(total, exchangeAvailable);
  const cardDue = arredondarMoeda(total - exchangeApplied);
  const cardPaid = arredondarMoeda(
    Object.values(state.checkout.cardAllocations).reduce(
      (sum, value) => sum + Number(value),
      0,
    ),
  );
  return {
    subtotal,
    discount,
    shipping,
    total,
    promo,
    selectedTrades,
    exchangeAvailable,
    exchangeApplied,
    cardDue,
    cardPaid,
    difference: arredondarMoeda(cardDue - cardPaid),
  };
}

export function finalizeOrder() {
  // Valida todos os pré-requisitos antes de criar o pedido.
  const owner = currentCustomer();
  if (!owner || !owner.active)
    throw Error("O cliente precisa estar ativo para comprar.");
  if (!cart().length) throw Error("Sua sacola está vazia.");
  const address = selectedAddress();
  if (!address) throw Error("Selecione um endereço de entrega.");
  const totals = checkoutTotals();
  if (Math.abs(totals.difference) > 0.009)
    throw Error("Distribua exatamente o valor restante entre os cartões.");
  if (totals.cardDue > 0 && !Object.keys(state.checkout.cardAllocations).length)
    throw Error("Selecione ao menos um cartão para o saldo restante.");
  if (cart().some((line) => line.q > line.p.stock))
    throw Error("A disponibilidade mudou. Revise a sacola.");
  let tradeRemaining = totals.exchangeApplied;
  const payments = [];

  // Consome cupons de troca por ordem até cobrir o crédito aplicado.
  for (const coupon of totals.selectedTrades) {
    const used = arredondarMoeda(Math.min(coupon.balance, tradeRemaining));
    if (used > 0) {
      coupon.balance = arredondarMoeda(coupon.balance - used);
      coupon.active = coupon.balance > 0;
      tradeRemaining = arredondarMoeda(tradeRemaining - used);
      payments.push({
        type: "CUPOM_TROCA",
        couponId: coupon.id,
        label: coupon.code,
        amount: used,
      });
    }
  }
  if (totals.promo) {
    // O uso promocional é contado apenas quando o pedido realmente é finalizado.
    totals.promo.uses += 1;
    payments.unshift({
      type: "CUPOM_PROMOCIONAL",
      couponId: totals.promo.id,
      label: totals.promo.code,
      amount: totals.discount,
    });
  }
  for (const [cardId, value] of Object.entries(
    state.checkout.cardAllocations,
  )) {
    // Registra pagamentos com cartão usando somente dados mascarados.
    const card = owner.cards.find((item) => item.id === cardId);
    if (card && value > 0)
      payments.push({
        type: "CARTAO",
        cardId,
        label: `${card.label} final ${card.last4}`,
        amount: arredondarMoeda(value),
      });
  }
  // Finalizar cria o pedido EM_ABERTO e limpa a sacola, mas ainda não baixa o estoque.
  // A baixa ocorre somente quando o administrador confirma o pagamento.
  const orderItem = {
    id: `#AUR-${25005 + state.orders.length + 1}`,
    customerId: owner.id,
    customerLabel: owner.name,
    date: dataAtualEmPtBr(),
    items: cart().map((line) => ({
      productId: line.p.id,
      quantity: line.q,
      unitPrice: line.p.price,
    })),
    subtotal: totals.subtotal,
    shipping: totals.shipping,
    discount: totals.discount,
    total: totals.total,
    status: "EM_ABERTO",
    address: clonarDados(address),
    payments,
    history: [{ status: "EM_ABERTO", at: agoraEmIso(), actor: "CLIENTE" }],
  };
  state.orders.unshift(orderItem);
  state.cart = {};
  state.reservation = null;
  state.checkout = {
    addressId:
      owner.addresses.find(
        (item) =>
          item.preferred && item.active && item.purposes?.includes("ENTREGA"),
      )?.id || null,
    newAddress: null,
    promoCouponId: null,
    exchangeCouponIds: [],
    cardAllocations: {},
    lastOrderId: orderItem.id,
  };
  audit(
    "FINALIZAR_PEDIDO",
    { orderId: orderItem.id, status: orderItem.status },
    "CLIENTE",
  );
  return orderItem;
}

export const order = (id) => state.orders.find((item) => item.id === id);
export const ordersForCustomer = (id) =>
  state.orders.filter((item) => item.customerId === id);
export const customerCoupons = (id) =>
  state.coupons.filter(
    (item) => item.customerId == null || item.customerId === id,
  );
export const exchangesForCustomer = (id) =>
  state.exchanges.filter((item) => item.customerId === id);
export const exchange = (id) => state.exchanges.find((item) => item.id === id);
const ORDER_TRANSITIONS = {
  EM_ABERTO: "EM_PROCESSAMENTO",
  EM_PROCESSAMENTO: "PAGAMENTO_REALIZADO",
  PAGAMENTO_REALIZADO: "EM_TRANSITO",
  EM_TRANSITO: "ENTREGUE",
};
export function progressOrder(id, actor = "ADMIN") {
  // Avança pedidos por uma máquina de estados simples e controlada.
  const item = order(id);
  if (item?.status === "EM_TRANSITO" && actor !== "CLIENTE")
    throw Error("A entrega deve ser confirmada pelo cliente.");
  const next = ORDER_TRANSITIONS[item?.status];
  if (!next) throw Error("Não existe próxima transição para este pedido.");
  if (next === "PAGAMENTO_REALIZADO" && !item.stockCommitted) {
    // Esta é a única transição que converte a reserva conceitual em baixa definitiva.
    for (const line of item.items)
      if (
        !product(line.productId) ||
        product(line.productId).stock < line.quantity
      )
        throw Error("Estoque insuficiente para confirmar o pagamento.");
    item.items.forEach((line) => {
      product(line.productId).stock -= line.quantity;
    });
    item.stockCommitted = true;
  }
  item.status = next;
  if (next === "ENTREGUE")
    item.deliveredAt = new Date().toISOString().slice(0, 10);
  item.history.push({ status: next, at: agoraEmIso(), actor });
  audit("ATUALIZAR_PEDIDO", { orderId: id, status: next }, actor);
  return item;
}
export function cancelOrder(id) {
  // Cliente só pode cancelar diretamente antes de pagamento confirmado.
  const item = order(id);
  if (!["EM_ABERTO", "EM_PROCESSAMENTO"].includes(item?.status))
    throw Error("Este pedido não pode mais ser cancelado diretamente.");
  item.status = "CANCELADO";
  item.history.push({
    status: "CANCELADO",
    at: agoraEmIso(),
    actor: "CLIENTE",
  });
  audit("CANCELAR_PEDIDO", { orderId: id }, "CLIENTE");
}
export function rejectPayment(id) {
  const item = order(id);
  if (item?.status !== "EM_PROCESSAMENTO")
    throw Error(
      "Somente pedidos em processamento podem ter o pagamento recusado.",
    );
  item.status = "CANCELADO";
  // A recusa cancela o pedido sem tocar no estoque, pois nenhuma baixa foi confirmada.
  item.cancelReason = "PAGAMENTO_RECUSADO";
  item.history.push({
    status: "CANCELADO",
    reason: item.cancelReason,
    at: agoraEmIso(),
    actor: "ADMIN",
  });
  audit(
    "RECUSAR_PAGAMENTO",
    {
      orderId: id,
      status: item.status,
      stockCommitted: Boolean(item.stockCommitted),
    },
    "ADMIN",
  );
  return item;
}
export function confirmReceipt(id) {
  // A última transição de entrega pertence ao cliente.
  if (order(id)?.status !== "EM_TRANSITO")
    throw Error(
      "Somente pedidos em trânsito podem ter recebimento confirmado.",
    );
  return progressOrder(id, "CLIENTE");
}

export function requestExchange(orderId, productId, quantity, reason) {
  // A troca possui ciclo próprio por item e quantidade; o pedido original continua ENTREGUE.
  const source = order(orderId);
  if (
    !source ||
    source.customerId !== state.currentCustomerId ||
    source.status !== "ENTREGUE"
  )
    throw Error("Somente itens de pedidos entregues podem ser trocados.");
  const line = source.items.find(
    (item) => item.productId === Number(productId),
  );
  const qty = Number(quantity);
  if (!line || qty < 1 || qty > line.quantity)
    throw Error("Informe uma quantidade válida do item.");
  if (!String(reason || "").trim()) throw Error("Informe o motivo da troca.");
  const item = {
    id: criarIdentificador("troca"),
    orderId,
    customerId: source.customerId,
    productId: Number(productId),
    quantity: qty,
    reason: reason.trim(),
    status: "TROCA_SOLICITADA",
    requestedAt: dataAtualEmPtBr(),
    history: [
      { status: "TROCA_SOLICITADA", at: agoraEmIso(), actor: "CLIENTE" },
    ],
  };
  state.exchanges.unshift(item);
  audit(
    "SOLICITAR_TROCA",
    { exchangeId: item.id, orderId, productId: item.productId },
    "CLIENTE",
  );
  return item;
}
export function decideExchange(id, accepted, justification = "") {
  // Administrador precisa aceitar ou negar uma troca recém-solicitada.
  const item = exchange(id);
  if (item?.status !== "TROCA_SOLICITADA")
    throw Error("Esta troca não aguarda decisão.");
  if (!accepted && !String(justification).trim())
    throw Error("Informe a justificativa da negativa.");
  item.status = accepted ? "TROCA_ACEITA" : "TROCA_NEGADA";
  item.justification = String(justification || "").trim();
  item.history.push({ status: item.status, at: agoraEmIso(), actor: "ADMIN" });
  audit("DECIDIR_TROCA", { exchangeId: id, status: item.status }, "ADMIN");
}
export function dispatchExchange(id, carrier, tracking) {
  // Depois da aprovação, o cliente informa como enviou o item de volta.
  const item = exchange(id);
  if (item?.status !== "TROCA_ACEITA")
    throw Error("Somente uma troca aceita pode ser despachada.");
  if (!String(carrier || "").trim() || !String(tracking || "").trim())
    throw Error("Informe transportadora e código de rastreio.");
  item.carrier = carrier.trim();
  item.tracking = tracking.trim();
  item.status = "ITEM_ENVIADO";
  item.history.push({
    status: item.status,
    at: agoraEmIso(),
    actor: "CLIENTE",
  });
  audit(
    "INFORMAR_DESPACHO_TROCA",
    { exchangeId: id, tracking: item.tracking },
    "CLIENTE",
  );
}
export function receiveExchange(id, restock) {
  // Recebimento pode devolver a quantidade ao estoque, conforme avaliação administrativa.
  const item = exchange(id);
  if (item?.status !== "ITEM_ENVIADO")
    throw Error("Somente item enviado pode ser recebido.");
  item.restock = Boolean(restock);
  item.status = "ITEM_RECEBIDO";
  if (item.restock) product(item.productId).stock += item.quantity;
  item.history.push({ status: item.status, at: agoraEmIso(), actor: "ADMIN" });
  audit(
    "RECEBER_ITEM_TROCA",
    { exchangeId: id, restock: item.restock },
    "ADMIN",
  );
}
export function processExchange(id) {
  const item = exchange(id);
  if (item?.status !== "ITEM_RECEBIDO")
    throw Error("Somente item recebido pode ter a troca processada.");
  if (item.generatedCouponId) throw Error("Esta troca já gerou cupom.");
  const value = arredondarMoeda(product(item.productId).price * item.quantity);
  // Somente o processamento de um item recebido pode gerar crédito de troca.
  // Por isso, este cupom não passa pelas rotinas de cadastro manual do administrador.
  const coupon = {
    id: criarIdentificador("trade"),
    customerId: item.customerId,
    code: `TROCA-${String(Date.now()).slice(-6)}`,
    type: "TROCA",
    mode: "CREDITO",
    value,
    balance: value,
    validUntil: "2027-08-31",
    active: true,
    sourceExchangeId: item.id,
  };
  state.coupons.unshift(coupon);
  item.generatedCouponId = coupon.id;
  item.status = "TROCA_PROCESSADA";
  item.history.push({ status: item.status, at: agoraEmIso(), actor: "ADMIN" });
  audit(
    "PROCESSAR_TROCA",
    { exchangeId: id, couponId: coupon.id, value },
    "ADMIN",
  );
  return coupon;
}

export function filterCustomers(filters = {}) {
  // Filtro genérico: cada campo preenchido precisa aparecer no cadastro do cliente.
  return state.customers.filter((item) =>
    Object.entries(filters).every(
      ([key, value]) =>
        !String(value || "").trim() ||
        String(item[key] || "")
          .toLowerCase()
          .includes(String(value).trim().toLowerCase()),
    ),
  );
}
export function filterOrders(filters = {}) {
  // Pedidos usam filtros específicos porque alguns campos são ids ou status exatos.
  return state.orders.filter(
    (item) =>
      (!filters.id ||
        item.id.toLowerCase().includes(filters.id.toLowerCase())) &&
      (!filters.customerId || item.customerId === filters.customerId) &&
      (!filters.status || item.status === filters.status),
  );
}
function normalizeProductData(data, editingId = null) {
  // Converte e valida campos do formulário antes de criar ou atualizar produto.
  const required = [
    "code",
    "name",
    "brand",
    "categories",
    "description",
    "price",
    "cost",
    "stock",
  ];
  if (required.some((key) => !String(data[key] ?? "").trim()))
    throw Error(
      "Preencha código, nome, marca, categorias, descrição, preço, custo e estoque.",
    );
  const code = String(data.code).trim().toUpperCase();
  if (
    state.products.some(
      (item) =>
        item.id !== Number(editingId) && item.code.toUpperCase() === code,
    )
  )
    throw Error("Já existe um produto com este código.");
  const categories = String(data.categories)
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean);
  if (!categories.length) throw Error("Informe ao menos uma categoria.");
  const price = Number(data.price);
  const cost = Number(data.cost);
  const stock = Number(data.stock);
  if (
    !Number.isFinite(price) ||
    price <= 0 ||
    !Number.isFinite(cost) ||
    cost < 0
  )
    throw Error("Informe preço e custo válidos.");
  if (!Number.isInteger(stock) || stock < 0)
    throw Error("O estoque deve ser um número inteiro maior ou igual a zero.");
  const image = String(data.image || "").trim();
  if (image && !/^(https?:\/\/|data:image\/|\/)/i.test(image))
    throw Error(
      "Informe uma URL de imagem válida ou selecione um arquivo de imagem.",
    );
  return {
    code,
    name: String(data.name).trim(),
    brand: String(data.brand).trim(),
    categories,
    price: arredondarMoeda(price),
    cost: arredondarMoeda(cost),
    tone: String(data.tone || "—").trim() || "—",
    finish: String(data.finish || "—").trim() || "—",
    skin: String(data.skin || "Todos os tipos").trim() || "Todos os tipos",
    volume: String(data.volume || "—").trim() || "—",
    ingredients:
      String(data.ingredients || "Não informado").trim() || "Não informado",
    stock,
    visible: Boolean(data.visible),
    description: String(data.description).trim(),
    art: /^#[0-9a-f]{6}$/i.test(String(data.art || ""))
      ? String(data.art)
      : "#b65d78",
    image,
  };
}
export function createProduct(data) {
  // O cadastro grava no mock local. Quando a API existir, esta função será substituída
  // por uma chamada HTTP sem mudar o formulário nem as regras de validação da interface.
  const nextId =
    Math.max(0, ...state.products.map((item) => Number(item.id) || 0)) + 1;
  const item = {
    id: nextId,
    ...normalizeProductData(data),
  };
  state.products.push(item);
  audit(
    "CADASTRAR_PRODUTO",
    { productId: item.id, code: item.code, visible: item.visible },
    "ADMIN_DEMONSTRACAO",
  );
  return item;
}
export function updateProduct(id, data) {
  // Edição altera dados atuais sem trocar o identificador do produto.
  const item = product(id);
  if (!item) throw Error("Produto não encontrado.");
  Object.assign(item, normalizeProductData(data, item.id));
  audit(
    "ALTERAR_PRODUTO",
    { productId: item.id, code: item.code, visible: item.visible },
    "ADMIN",
  );
  return item;
}
export function toggleProductVisibility(id) {
  // Visibilidade controla o catálogo sem apagar o produto do cadastro administrativo.
  const item = product(id);
  item.visible = !item.visible;
  audit(
    item.visible ? "REATIVAR_PRODUTO" : "OCULTAR_PRODUTO",
    { productId: item.id },
    "ADMIN",
  );
}
export function resetCheckout() {
  // Volta o checkout para o endereço preferencial do cliente e limpa pagamentos temporários.
  const owner = currentCustomer();
  state.checkout = {
    addressId:
      owner?.addresses.find(
        (item) =>
          item.preferred && item.active && item.purposes?.includes("ENTREGA"),
      )?.id || null,
    newAddress: null,
    promoCouponId: null,
    exchangeCouponIds: [],
    cardAllocations: {},
    lastOrderId: state.checkout?.lastOrderId || null,
  };
  persist();
}
