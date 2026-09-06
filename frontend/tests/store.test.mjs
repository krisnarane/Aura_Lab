import assert from "node:assert/strict";

const memory = new Map();
globalThis.localStorage = {
  getItem: (key) => (memory.has(key) ? memory.get(key) : null),
  setItem: (key, value) => memory.set(key, String(value)),
  removeItem: (key) => memory.delete(key),
};

const store = await import("../js/store.js");
const pages = await import("../js/pages.js");
const identificarClienteMock = (id = "cli-1") => {
  store.state.currentCustomerId = id;
  store.persist();
};

store.resetDemo();
assert.equal(
  store.customers().length,
  3,
  "massa contém clientes ativos e inativo",
);
assert.equal(store.currentCustomer().id, "cli-1", "cliente mockado inicia identificado");
assert.equal(memory.size, 1, "estado demonstrativo fica no localStorage");
assert.equal(JSON.parse(memory.get("aura-lab-v3")).currentCustomerId, "cli-1");
assert.equal(
  store.currentCustomer().addresses.length,
  2,
  "cliente possui vários endereços",
);
assert.equal(
  store.currentCustomer().cards.filter((card) => card.preferred).length,
  1,
  "há um cartão preferencial",
);

const currentCustomerBeforeAdminCreation = store.currentCustomer().id;
const adminCreated = store.createCustomerByAdmin({
  name: "Cliente do Admin",
  cpf: "222.333.444-55",
  email: "admin-criou@teste.dev",
  password: "123456",
  gender: "Outro",
  birthDate: "1990-05-10",
  phoneType: "Celular",
  phoneDdd: "41",
  phoneNumber: "95555-4444",
  addressLabel: "Casa",
  residenceType: "Casa",
  streetType: "Rua",
  street: "Administrativa",
  number: "10",
  district: "Centro",
  cep: "80000-000",
  city: "Curitiba",
  state: "PR",
  country: "Brasil",
});
assert.equal(
  store.currentCustomer().id,
  currentCustomerBeforeAdminCreation,
  "cadastro pelo admin não troca o cliente corrente",
);
store.updateCustomer(
  adminCreated.id,
  { ...adminCreated, name: "Cliente Editado pelo Admin" },
  "ADMIN",
);
assert.equal(
  store.customer(adminCreated.id).name,
  "Cliente Editado pelo Admin",
  "admin edita dados do cliente",
);
store.inactivateCustomer(adminCreated.id, "ADMIN");
assert.equal(
  store.customer(adminCreated.id).active,
  false,
  "admin inativa cliente preservado no estado",
);

store.resetDemo();

const created = store.createCustomer({
  name: "Cliente Teste",
  cpf: "111.222.333-44",
  email: "cliente@teste.dev",
  gender: "Feminino",
  birthDate: "2000-01-01",
  phoneType: "Celular",
  phoneDdd: "11",
  phoneNumber: "91111-2222",
  password: "123456",
  addressLabel: "Casa",
  residenceType: "Apartamento",
  streetType: "Rua",
  street: "Teste",
  number: "1",
  district: "Centro",
  cep: "01000-000",
  city: "São Paulo",
  state: "SP",
  country: "Brasil",
});
assert.equal(created.active, true, "cadastro cria cliente ativo");
assert.equal(
  JSON.parse(memory.get("aura-lab-v3")).currentCustomerId,
  created.id,
  "cadastro atualiza o cliente demonstrativo no localStorage",
);
assert.deepEqual([...memory.keys()], ["aura-lab-v3"]);
store.updateCustomer(created.id, { ...created, name: "Cliente Alterado" });
assert.equal(
  store.customer(created.id).name,
  "Cliente Alterado",
  "cliente pode ser alterado",
);
store.saveAddress(created.id, {
  label: "Trabalho",
  residenceType: "Comercial",
  streetType: "Rua",
  cep: "01000-000",
  street: "Teste",
  number: "2",
  district: "Centro",
  city: "São Paulo",
  state: "SP",
  country: "Brasil",
  delivery: true,
  preferred: false,
});
store.saveCard(created.id, {
  brand: "Visa",
  holder: "CLIENTE TESTE",
  number: "4111111111111234",
  securityCode: "123",
  preferred: true,
});
assert.equal(
  store.customer(created.id).addresses.length,
  2,
  "endereço associado ao cliente",
);
assert.equal(
  store.customer(created.id).cards.length,
  1,
  "cartão associado ao cliente",
);
store.inactivateCustomer(created.id);
assert.equal(
  store.customer(created.id).active,
  false,
  "inativação preserva cliente",
);

store.resetDemo();
assert.equal(
  store.add(1, 2),
  true,
  "produto pode ser adicionado com quantidade",
);
assert.equal(store.cart()[0].q, 2, "carrinho mantém quantidade");
store.amount(1, -1);
assert.equal(store.cart()[0].q, 1, "quantidade pode ser alterada");
store.removeCartItem(1);
assert.equal(store.cart().length, 0, "item pode ser removido");

const productIdBeforeEdit = store.product(1).id;
store.updateProduct(1, {
  ...store.product(1),
  name: "Base Sérum Editada",
  categories: store.product(1).categories.join(", "),
});
assert.equal(store.product(1).id, productIdBeforeEdit, "edição preserva o produto");
assert.equal(
  store.product(1).name,
  "Base Sérum Editada",
  "admin edita produto cadastrado",
);

store.resetDemo();
store.state.currentCustomerId = null;
assert.throws(
  () => store.setCheckoutAddress({ addressId: "addr-1" }),
  /cliente demonstrativo/,
  "checkout exige cliente demonstrativo identificado",
);

store.resetDemo();
identificarClienteMock();

store.add(3, 1);
store.setCheckoutAddress({ addressId: "addr-1" });
store.setCheckoutPayments({
  promoCode: "AURA10",
  exchangeCouponIds: ["trade-1"],
  cardAllocations: {},
});
const beforeAllocation = store.checkoutTotals();
assert.ok(beforeAllocation.discount > 0, "cupom promocional reduz preço");
assert.equal(
  beforeAllocation.exchangeApplied,
  50,
  "cupom de troca entra como crédito",
);
store.setCheckoutPayments({
  promoCode: "AURA10",
  exchangeCouponIds: ["trade-1"],
  cardAllocations: { "card-1": beforeAllocation.cardDue },
});
const combined = store.checkoutTotals();
assert.equal(combined.difference, 0, "cartão completa exatamente o saldo");
const newOrder = store.finalizeOrder();
assert.equal(newOrder.status, "EM_ABERTO", "pedido nasce EM_ABERTO");
assert.equal(
  newOrder.payments.some((item) => item.type === "CUPOM_TROCA"),
  true,
  "pedido registra cupom de troca",
);
assert.equal(
  newOrder.payments.some((item) => item.type === "CARTAO"),
  true,
  "pedido registra cartão",
);

store.progressOrder(newOrder.id);
assert.equal(store.order(newOrder.id).status, "EM_PROCESSAMENTO");
const stockBeforePayment = store.product(3).stock;
store.progressOrder(newOrder.id);
assert.equal(store.order(newOrder.id).status, "PAGAMENTO_REALIZADO");
assert.equal(
  store.product(3).stock,
  stockBeforePayment - 1,
  "pagamento confirmado baixa estoque",
);
store.progressOrder(newOrder.id);
assert.equal(store.order(newOrder.id).status, "EM_TRANSITO");
store.confirmReceipt(newOrder.id);
assert.equal(
  store.order(newOrder.id).status,
  "ENTREGUE",
  "cliente confirma recebimento",
);

store.resetDemo();
identificarClienteMock();
store.cancelOrder("#AUR-25001");
assert.equal(
  store.order("#AUR-25001").status,
  "CANCELADO",
  "cliente cancela pedido aberto",
);

const requested = store.requestExchange(
  "#AUR-25005",
  3,
  1,
  "Quero outra tonalidade",
);
assert.equal(requested.status, "TROCA_SOLICITADA");
store.decideExchange(requested.id, true);
assert.equal(store.exchange(requested.id).status, "TROCA_ACEITA");
store.dispatchExchange(requested.id, "Correios", "BR000111222");
assert.equal(store.exchange(requested.id).status, "ITEM_ENVIADO");
const exchangeStockBefore = store.product(3).stock;
store.receiveExchange(requested.id, true);
assert.equal(store.exchange(requested.id).status, "ITEM_RECEBIDO");
assert.equal(
  store.product(3).stock,
  exchangeStockBefore + 1,
  "item elegível retorna ao estoque",
);
const generated = store.processExchange(requested.id);
assert.equal(store.exchange(requested.id).status, "TROCA_PROCESSADA");
assert.equal(generated.type, "TROCA", "processamento gera cupom de troca");
assert.equal(
  generated.balance,
  store.product(3).price,
  "cupom corresponde ao valor trocado",
);

assert.equal(
  store.filterCustomers({ name: "Marina", cpf: "123" }).length,
  1,
  "filtros de cliente podem ser combinados",
);
store.resetDemo();
identificarClienteMock();
assert.match(
  pages.registrationPage(),
  /Senha demonstrativa/,
  "cadastro mantém senha demonstrativa",
);
assert.match(
  pages.registrationPage(),
  /customer-create-form/,
  "há tela de cadastro",
);
assert.match(
  pages.checkoutPayment(),
  /exchangeCoupon/,
  "checkout lista cupons de troca",
);
assert.match(
  pages.checkoutAddress(),
  /new-address-fields hidden-section/,
  "formulário de novo endereço inicia oculto",
);
assert.match(
  pages.checkoutPayment(),
  /card_card-1/,
  "checkout permite valor por cartão",
);
assert.match(pages.profileOrders(), /Meus pedidos/, "cliente consulta pedidos");
assert.match(
  pages.adminPage("Clientes", new URLSearchParams()),
  /admin-customer-filter/,
  "admin consulta clientes",
);
assert.match(
  pages.adminPage("Clientes", new URLSearchParams()),
  /Voltar para a loja/,
  "menu administrativo permite retornar à loja",
);
assert.match(
  pages.adminPage("Clientes", new URLSearchParams("new=1")),
  /admin-customer-form/,
  "admin acessa cadastro de cliente",
);
assert.match(
  pages.adminPage("Clientes", new URLSearchParams("edit=cli-1")),
  /value="Marina Costa"/,
  "admin acessa edição de cliente preenchida",
);
assert.match(
  pages.adminPage("Produtos", new URLSearchParams("edit=1")),
  /Editar produto/,
  "admin acessa edição de produto cadastrado",
);
assert.match(
  pages.adminPage("Trocas", new URLSearchParams()),
  /TROCA SOLICITADA/,
  "admin consulta trocas",
);
assert.match(
  pages.adminPage(
    "Dashboard",
    new URLSearchParams("start=2026-06-01&end=2026-08-01&category=Blush&category=Bruma"),
  ),
  /Exportar dados/,
  "dashboard oferece exportação dos dados filtrados",
);
const analysis = store.salesAnalysis({
  startDate: "2026-06-01",
  endDate: "2026-08-31",
  categories: ["Blush", "Bruma"],
});
assert.equal(analysis.periods.length, 3, "análise respeita o período informado");
assert.equal(analysis.maxDate, "2026-08-31", "data final aceita o mês inteiro");
assert.deepEqual(
  analysis.series.map((item) => item.category),
  ["Blush", "Bruma"],
  "análise mantém as categorias selecionadas",
);
assert.throws(
  () =>
    store.salesAnalysis({ startDate: "2026-08-01", endDate: "2026-06-01" }),
  /data final não pode ser anterior/,
  "análise rejeita data final anterior à inicial",
);
assert.match(
  pages.advisorChat(),
  /aura-chat-messages/,
  "chatbot permanece disponível",
);

console.log("Regras canônicas do protótipo validadas.");
