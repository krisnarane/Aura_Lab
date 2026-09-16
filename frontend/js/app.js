/**
 * Ponto de entrada da SPA: resolve rotas, renderiza a tela e liga seus eventos.
 * Regras de negócio permanecem no store e comportamentos especializados em módulos próprios.
 */
import {
  state as estado,
  currentCustomer as obterClienteAtual,
  add as adicionarProdutoAoCarrinho,
  amount as alterarQuantidadeNoCarrinho,
  removeCartItem as removerItemDoCarrinho,
  createCustomer as cadastrarCliente,
  createCustomerByAdmin as cadastrarClientePeloAdmin,
  updateCustomer as atualizarCliente,
  changePassword as alterarSenha,
  inactivateCustomer as inativarCliente,
  saveAddress as salvarEndereco,
  setPreferredAddress as definirEnderecoPreferencial,
  inactivateAddress as inativarEndereco,
  saveCard as salvarCartao,
  setPreferredCard as definirCartaoPreferencial,
  inactivateCard as inativarCartao,
  setCheckoutAddress as definirEnderecoCheckout,
  setCheckoutPayments as definirPagamentosCheckout,
  checkoutTotals as calcularTotaisCheckout,
  finalizeOrder as finalizarPedido,
  cancelOrder as cancelarPedido,
  confirmReceipt as confirmarRecebimento,
  requestExchange as solicitarTroca,
  dispatchExchange as despacharTroca,
  progressOrder as avancarPedido,
  rejectPayment as recusarPagamento,
  decideExchange as decidirTroca,
  receiveExchange as receberTroca,
  processExchange as processarTroca,
  createProduct as cadastrarProduto,
  updateProduct as editarProduto,
  toggleProductVisibility as alternarVisibilidadeProduto,
  savePromotionalCoupon as salvarCupomPromocional,
  inactivatePromotionalCoupon as inativarCupomPromocional,
  resetDemo as restaurarDemonstracao,
} from "./store.js";
import {
  catalog as criarCatalogo,
  detail as criarDetalheProduto,
  cartPage as criarPaginaCarrinho,
  checkoutAddress as criarCheckoutEndereco,
  checkoutPayment as criarCheckoutPagamento,
  checkoutReview as criarCheckoutRevisao,
  checkoutConfirmation as criarCheckoutConfirmacao,
  registrationPage as criarPaginaCadastro,
  profileData as criarPerfilDados,
  profileSecurity as criarPerfilSeguranca,
  profileAddresses as criarPerfilEnderecos,
  profileCards as criarPerfilCartoes,
  profileOrders as criarPerfilPedidos,
  profileOrderDetail as criarDetalhePedidoPerfil,
  profileCoupons as criarPerfilCupons,
  advisorChat as criarChatConsultora,
  adminPage as criarPaginaAdmin,
  notFound as criarPaginaNaoEncontrada,
} from "./pages.js";
import { aplicarDetalhesVisuais } from "./interface.js";
import { inicializarConsultoraAura } from "./consultora-aura.js";
import { inicializarAnalytics } from "./analytics.js";
import { ehRotaClientesAdmin, renderizarClientesAdmin } from "./admin-clientes.js";
import { ehRotaProdutosAdmin, renderizarProdutosAdmin } from "./admin-produtos.js";

const raizAplicacao = document.querySelector("#app");

// Mapeia o nome técnico da rota administrativa para o título usado no menu.
const SECOES_ADMIN = {
  dashboard: "Dashboard",
  clientes: "Clientes",
  pedidos: "Pedidos",
  trocas: "Trocas",
  produtos: "Produtos",
  cupons: "Cupons",
  configuracoes: "Configurações",
};

function exibirToast(message, kind = "info") {
  // Cria uma notificação curta no canto da tela e remove automaticamente.
  document
    .querySelector("#toast-area")
    ?.insertAdjacentHTML(
      "beforeend",
      `<div class="toast show aura-toast ${kind === "error" ? "is-error" : ""}" role="status">${String(message).replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;")}</div>`,
    );
  setTimeout(() => document.querySelector(".aura-toast")?.remove(), 3600);
}

function navegar(href) {
  // Atualiza a URL sem recarregar a página; depois renderiza a tela correspondente.
  history.pushState({}, "", href);
  renderizar();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function resolverPagina() {
  // Converte pathname e query string na função de tela correta.
  const path = location.pathname;
  const query = new URLSearchParams(location.search);
  const parts = path.split("/").filter(Boolean);
  if (path === "/" || path === "/catalogo") return criarCatalogo(query);
  if (parts[0] === "produto" && parts[1]) return criarDetalheProduto(parts[1]);
  if (path === "/sacola") return criarPaginaCarrinho();
  if (path === "/cadastro") return criarPaginaCadastro();
  if (path === "/checkout" || path === "/checkout/endereco")
    return criarCheckoutEndereco();
  if (path === "/checkout/pagamento") return criarCheckoutPagamento();
  if (path === "/checkout/revisao") return criarCheckoutRevisao();
  if (path === "/checkout/confirmacao") return criarCheckoutConfirmacao();
  if (path === "/perfil" || path === "/perfil/pedidos")
    return criarPerfilPedidos();
  if (path === "/perfil/dados") return criarPerfilDados();
  if (path === "/perfil/seguranca") return criarPerfilSeguranca();
  if (path === "/perfil/enderecos") return criarPerfilEnderecos(query);
  if (path === "/perfil/cartoes") return criarPerfilCartoes();
  if (path === "/perfil/cupons") return criarPerfilCupons();
  if (parts[0] === "perfil" && parts[1] === "pedidos" && parts[2])
    return criarDetalhePedidoPerfil(
      decodeURIComponent(parts.slice(2).join("/")),
    );
  if (path === "/consultora") return criarChatConsultora();
  if (parts[0] === "admin") {
    const section = SECOES_ADMIN[parts[1]] || "Dashboard";
    const id = parts[2] ? decodeURIComponent(parts.slice(2).join("/")) : null;
    return criarPaginaAdmin(section, query, id);
  }
  return criarPaginaNaoEncontrada();
}

function renderizar() {
  // Toda navegação redesenha o HTML, reaplica detalhes visuais e religa eventos.
  if (ehRotaClientesAdmin()) {
    renderizarClientesAdmin(raizAplicacao, navegar, exibirToast);
    return;
  }
  if (ehRotaProdutosAdmin()) {
    renderizarProdutosAdmin(raizAplicacao, navegar, exibirToast);
    return;
  }
  raizAplicacao.innerHTML = resolverPagina();
  aplicarDetalhesVisuais(raizAplicacao);
  registrarEventos();

  // Na sacola, agenda nova renderização quando a reserva chegar ao vencimento.
  clearTimeout(window.auraReservationTimer);
  if (location.pathname === "/sacola" && estado.reservation) {
    window.auraReservationTimer = setTimeout(
      renderizar,
      Math.min(
        Math.max(estado.reservation.expiresAt - Date.now(), 0) + 50,
        60000,
      ),
    );
  }
}

// Transforma os campos de um formulário em objeto comum para enviar ao store.
const lerDadosFormulario = (form) => Object.fromEntries(new FormData(form));

// Converte uma imagem escolhida no input file em Data URL para pré-visualização e mock local.
const lerArquivoImagem = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () =>
      reject(Error("Não foi possível ler a imagem selecionada."));
    reader.readAsDataURL(file);
  });

function executarAcao(action, success, deveRenderizar = true) {
  // Envolve ações de negócio para mostrar erro/sucesso e evitar repetição nos listeners.
  try {
    const result = action();
    if (success) exibirToast(success);
    if (deveRenderizar) renderizar();
    return result;
  } catch (error) {
    exibirToast(error.message, "error");
    return null;
  }
}

function coletarPagamento(form) {
  // Agrupa valores dos cartões em um objeto indexado pelo id de cada cartão.
  const data = new FormData(form);
  const cardAllocations = {};
  for (const [key, value] of data.entries())
    if (key.startsWith("card_"))
      cardAllocations[key.slice(5)] = Number(value || 0);
  return {
    promoCode: data.get("promoCode"),
    exchangeCouponIds: data.getAll("exchangeCoupon"),
    cardAllocations,
  };
}

function registrarEventos() {
  // Links internos usam navegação da SPA em vez de recarregar o documento inteiro.
  document.querySelectorAll("[data-link]").forEach((link) =>
    link.addEventListener("click", (event) => {
      event.preventDefault();
      navegar(link.getAttribute("href"));
    }),
  );
  document
    .querySelectorAll("[data-product]")
    .forEach((button) =>
      button.addEventListener("click", () =>
        navegar(`/produto/${button.dataset.product}`),
      ),
    );

  // Botões do catálogo levam para detalhe ou adicionam itens na sacola.
  document.querySelectorAll("[data-add]").forEach((button) =>
    button.addEventListener("click", () => {
      const ok = adicionarProdutoAoCarrinho(button.dataset.add, 1);
      exibirToast(
        ok ? "Produto reservado por 20 minutos." : "Quantidade indisponível.",
        ok ? "info" : "error",
      );
      if (ok) renderizar();
    }),
  );
  document.querySelectorAll("[data-add-detail]").forEach((button) =>
    button.addEventListener("click", () => {
      const quantity = Number(
        document.querySelector("#product-quantity")?.value || 1,
      );
      const ok = adicionarProdutoAoCarrinho(button.dataset.addDetail, quantity);
      exibirToast(
        ok
          ? `${quantity} item(ns) adicionado(s) à sacola.`
          : "Quantidade indisponível.",
        ok ? "info" : "error",
      );
      if (ok) navegar("/sacola");
    }),
  );

  // Controles da sacola alteram quantidades ou removem itens e redesenham a tela.
  document.querySelectorAll("[data-qty]").forEach((button) =>
    button.addEventListener("click", () => {
      alterarQuantidadeNoCarrinho(button.dataset.qty, Number(button.dataset.d));
      renderizar();
    }),
  );
  document.querySelectorAll("[data-remove-cart]").forEach((button) =>
    button.addEventListener("click", () => {
      removerItemDoCarrinho(button.dataset.removeCart);
      renderizar();
    }),
  );

  // Filtros e busca são refletidos na URL para manter a tela reproduzível.
  document
    .querySelectorAll('[name="cat"]')
    .forEach((input) =>
      input.addEventListener("change", () =>
        navegar(`/?category=${encodeURIComponent(input.value)}`),
      ),
    );
  document.querySelectorAll("[data-filter]").forEach((input) =>
    input.addEventListener("change", () => {
      const query = new URLSearchParams(location.search);
      input.value
        ? query.set(input.dataset.filter, input.value)
        : query.delete(input.dataset.filter);
      navegar(`/?${query.toString()}`);
    }),
  );
  document
    .querySelector("#search")
    ?.addEventListener("change", (event) =>
      navegar(`/?q=${encodeURIComponent(event.target.value)}`),
    );

  // Cadastro e perfil do cliente: cada formulário chama uma regra do store.
  document
    .querySelector("#customer-create-form")
    ?.addEventListener("submit", (event) => {
      event.preventDefault();
      const item = executarAcao(
        () => cadastrarCliente(lerDadosFormulario(event.target)),
        "Cliente cadastrado.",
      );
      if (item) navegar("/perfil/dados");
    });
  document
    .querySelector("#customer-profile-form")
    ?.addEventListener("submit", (event) => {
      event.preventDefault();
      executarAcao(
        () =>
          atualizarCliente(
            obterClienteAtual().id,
            lerDadosFormulario(event.target),
          ),
        "Dados cadastrais atualizados.",
      );
    });

  // Endereços preservam regras como finalidade, preferência e endereço mínimo ativo.
  document
    .querySelector("#password-form")
    ?.addEventListener("submit", (event) => {
      event.preventDefault();
      executarAcao(
        () => alterarSenha(obterClienteAtual().id, lerDadosFormulario(event.target)),
        "Senha demonstrativa atualizada.",
      );
    });
  document
    .querySelector("[data-inactivate-customer]")
    ?.addEventListener("click", () => {
      if (
        !confirm(
          "Inativar a conta? O histórico será preservado e novas compras ficarão bloqueadas.",
        )
      )
        return;
      executarAcao(
        () => inativarCliente(obterClienteAtual().id),
        "Conta inativada.",
      );
    });
  document
    .querySelector("#address-form")
    ?.addEventListener("submit", (event) => {
      event.preventDefault();
      const data = lerDadosFormulario(event.target);
      const editing = data.addressId || null;
      const item = executarAcao(
        () =>
          salvarEndereco(
            obterClienteAtual().id,
            {
              ...data,
              billing: Boolean(data.billing),
              delivery: Boolean(data.delivery),
              preferred: Boolean(data.preferred),
            },
            editing,
          ),
        editing ? "Endereço atualizado." : "Endereço cadastrado.",
        false,
      );
      if (item) navegar("/perfil/enderecos");
    });
  document
    .querySelectorAll("[data-preferred-address]")
    .forEach((button) =>
      button.addEventListener("click", () =>
        executarAcao(
          () =>
            definirEnderecoPreferencial(
              obterClienteAtual().id,
              button.dataset.preferredAddress,
            ),
          "Endereço preferencial atualizado.",
        ),
      ),
    );
  document
    .querySelectorAll("[data-inactivate-address]")
    .forEach((button) =>
      button.addEventListener("click", () =>
        executarAcao(
          () =>
            inativarEndereco(
              obterClienteAtual().id,
              button.dataset.inactivateAddress,
            ),
          "Endereço inativado.",
        ),
      ),
    );

  // Cartões salvam apenas dados mascarados; o número completo não vai para o estado.
  document.querySelector("#card-form")?.addEventListener("submit", (event) => {
    event.preventDefault();
    const data = lerDadosFormulario(event.target);
    executarAcao(
      () =>
        salvarCartao(obterClienteAtual().id, {
          ...data,
          preferred: Boolean(data.preferred),
        }),
      "Cartão cadastrado com dados sensíveis descartados.",
    );
  });
  document
    .querySelectorAll("[data-preferred-card]")
    .forEach((button) =>
      button.addEventListener("click", () =>
        executarAcao(
          () =>
            definirCartaoPreferencial(
              obterClienteAtual().id,
              button.dataset.preferredCard,
            ),
          "Cartão preferencial atualizado.",
        ),
      ),
    );
  document
    .querySelectorAll("[data-inactivate-card]")
    .forEach((button) =>
      button.addEventListener("click", () =>
        executarAcao(
          () =>
            inativarCartao(
              obterClienteAtual().id,
              button.dataset.inactivateCard,
            ),
          "Cartão inativado.",
        ),
      ),
    );

  // Checkout de endereço alterna entre endereço salvo e novo endereço preenchido na hora.
  const checkoutAddressForm = document.querySelector("#checkout-address-form");
  const newAddressFields = checkoutAddressForm?.querySelector(
    ".new-address-fields",
  );
  const atualizarCamposNovoEndereco = () => {
    if (!checkoutAddressForm || !newAddressFields) return;
    const selected = checkoutAddressForm.querySelector(
      '[name="addressId"]:checked',
    )?.value;
    const shouldShow = selected === "new";
    newAddressFields.classList.toggle("hidden-section", !shouldShow);
    newAddressFields
      .querySelectorAll("input, select, textarea")
      .forEach((field) => (field.disabled = !shouldShow));
  };
  checkoutAddressForm
    ?.querySelectorAll('[name="addressId"]')
    .forEach((input) =>
      input.addEventListener("change", atualizarCamposNovoEndereco),
    );
  atualizarCamposNovoEndereco();
  checkoutAddressForm
    ?.addEventListener("submit", (event) => {
      event.preventDefault();
      const data = lerDadosFormulario(event.target);
      const ok = executarAcao(
        () =>
          definirEnderecoCheckout({
            ...data,
            saveAddress: Boolean(data.saveAddress),
          }),
        null,
        false,
      );
      if (ok !== null) navegar("/checkout/pagamento");
    });

  // Checkout de pagamento permite cupom promocional, cupons de troca e vários cartões.
  document
    .querySelector("[data-toggle-new-card]")
    ?.addEventListener("click", () =>
      document
        .querySelector("#checkout-new-card-form")
        ?.classList.toggle("hidden-section"),
    );
  document
    .querySelector("#checkout-new-card-form")
    ?.addEventListener("submit", (event) => {
      event.preventDefault();
      const data = lerDadosFormulario(event.target);
      executarAcao(
        () =>
          salvarCartao(obterClienteAtual().id, {
            ...data,
            preferred: Boolean(data.preferred),
          }),
        "Cartão adicionado ao perfil.",
      );
    });
  document
    .querySelector("[data-apply-payment]")
    ?.addEventListener("click", () => {
      const form = document.querySelector("#checkout-payment-form");
      executarAcao(
        () => definirPagamentosCheckout(coletarPagamento(form)),
        "Pagamento recalculado.",
      );
    });
  document
    .querySelector("[data-auto-allocate]")
    ?.addEventListener("click", () => {
      const form = document.querySelector("#checkout-payment-form");
      try {
        // Primeiro recalcula cupons; depois coloca o saldo restante no cartão preferencial.
        definirPagamentosCheckout(coletarPagamento(form));
        const totals = calcularTotaisCheckout();
        const cards = obterClienteAtual().cards.filter((item) => item.active);
        const preferred = cards.find((item) => item.preferred) || cards[0];
        const allocation = Object.fromEntries(
          cards.map((item) => [
            item.id,
            item.id === preferred?.id ? totals.cardDue : 0,
          ]),
        );
        definirPagamentosCheckout({
          ...coletarPagamento(form),
          cardAllocations: allocation,
        });
        exibirToast("Valor restante colocado no cartão preferencial.");
        renderizar();
      } catch (error) {
        exibirToast(error.message, "error");
      }
    });
  document
    .querySelector("#checkout-payment-form")
    ?.addEventListener("submit", (event) => {
      event.preventDefault();
      const ok = executarAcao(
        () => definirPagamentosCheckout(coletarPagamento(event.target)),
        null,
        false,
      );
      if (ok !== null) {
        const totals = calcularTotaisCheckout();
        if (Math.abs(totals.difference) >= 0.01)
          return exibirToast(
            "A composição ainda não fecha o total. Ajuste os valores dos cartões.",
            "error",
          );
        navegar("/checkout/revisao");
      }
    });

  // Finalização cria o pedido e segue para a tela de confirmação.
  document
    .querySelector("[data-finalize-order]")
    ?.addEventListener("click", () => {
      const item = executarAcao(
        () => finalizarPedido(),
        "Pedido criado em EM ABERTO.",
        false,
      );
      if (item) navegar("/checkout/confirmacao");
    });

  // Ações do cliente sobre pedidos e trocas seguem o ciclo demonstrativo de compra.
  document.querySelectorAll("[data-cancel-order]").forEach((button) =>
    button.addEventListener("click", () => {
      if (!confirm("Cancelar este pedido?")) return;
      executarAcao(
        () => cancelarPedido(button.dataset.cancelOrder),
        "Pedido cancelado.",
      );
    }),
  );
  document.querySelectorAll("[data-confirm-receipt]").forEach((button) =>
    button.addEventListener("click", () => {
      if (!confirm("Confirmar que você recebeu este pedido?")) return;
      executarAcao(
        () => confirmarRecebimento(button.dataset.confirmReceipt),
        "Recebimento confirmado; pedido ENTREGUE.",
      );
    }),
  );
  document.querySelectorAll(".exchange-request-form").forEach((form) =>
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      const data = lerDadosFormulario(form);
      executarAcao(
        () =>
          solicitarTroca(
            form.dataset.orderId,
            Number(form.dataset.productId),
            Number(data.quantity),
            data.reason,
          ),
        "Troca solicitada.",
      );
    }),
  );
  document.querySelectorAll(".exchange-dispatch-form").forEach((form) =>
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      const data = lerDadosFormulario(form);
      executarAcao(
        () =>
          despacharTroca(form.dataset.exchangeId, data.carrier, data.tracking),
        "Despacho informado; item marcado como enviado.",
      );
    }),
  );

  // Área administrativa de clientes: filtros, criação, edição e inativação.
  document
    .querySelector("#admin-customer-filter")
    ?.addEventListener("submit", (event) => {
      event.preventDefault();
      navegar(
        `/admin/clientes?${new URLSearchParams(lerDadosFormulario(event.target)).toString()}`,
      );
    });
  document
    .querySelector("#admin-customer-form")
    ?.addEventListener("submit", (event) => {
      event.preventDefault();
      const data = lerDadosFormulario(event.target);
      const editing = data.customerId || null;
      const item = executarAcao(
        () =>
          editing
            ? atualizarCliente(editing, data, "ADMIN")
            : cadastrarClientePeloAdmin(data),
        editing ? "Cliente atualizado." : "Cliente cadastrado.",
        false,
      );
      if (item !== null) navegar("/admin/clientes");
    });
  document
    .querySelectorAll("[data-admin-inactivate-customer]")
    .forEach((button) =>
      button.addEventListener("click", () => {
        if (
          !confirm(
            "Inativar este cliente? Pedidos, cartões, endereços e histórico serão preservados.",
          )
        )
          return;
        executarAcao(
          () =>
            inativarCliente(
              button.dataset.adminInactivateCustomer,
              "ADMIN",
            ),
          "Cliente inativado.",
        );
      }),
    );

  // Área administrativa de pedidos: filtros e transições de status permitidas.
  document
    .querySelector("#admin-order-filter")
    ?.addEventListener("submit", (event) => {
      event.preventDefault();
      navegar(
        `/admin/pedidos?${new URLSearchParams(lerDadosFormulario(event.target)).toString()}`,
      );
    });
  document
    .querySelectorAll("[data-progress-order]")
    .forEach((button) =>
      button.addEventListener("click", () =>
        executarAcao(
          () => avancarPedido(button.dataset.progressOrder),
          "Status do pedido atualizado.",
        ),
      ),
    );
  document.querySelectorAll("[data-reject-payment]").forEach((button) =>
    button.addEventListener("click", () => {
      if (
        !confirm(
          "Simular a recusa da operadora? O pedido será cancelado sem baixar o estoque.",
        )
      )
        return;
      executarAcao(
        () => recusarPagamento(button.dataset.rejectPayment),
        "Pagamento recusado; itens mantidos em estoque.",
      );
    }),
  );

  // Área administrativa de trocas: aprova, nega, recebe e processa solicitações.
  document
    .querySelectorAll("[data-accept-exchange]")
    .forEach((button) =>
      button.addEventListener("click", () =>
        executarAcao(
          () => decidirTroca(button.dataset.acceptExchange, true),
          "Troca aceita.",
        ),
      ),
    );
  document.querySelectorAll("[data-deny-exchange]").forEach((button) =>
    button.addEventListener("click", () => {
      const justification = prompt("Justificativa da negativa:");
      if (justification == null) return;
      executarAcao(
        () => decidirTroca(button.dataset.denyExchange, false, justification),
        "Troca negada.",
      );
    }),
  );
  document.querySelectorAll(".receive-exchange-form").forEach((form) =>
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      const data = lerDadosFormulario(form);
      executarAcao(
        () => receberTroca(form.dataset.exchangeId, Boolean(data.restock)),
        "Item recebido.",
      );
    }),
  );
  document
    .querySelectorAll("[data-process-exchange]")
    .forEach((button) =>
      button.addEventListener("click", () =>
        executarAcao(
          () => processarTroca(button.dataset.processExchange),
          "Troca processada e cupom gerado.",
        ),
      ),
    );

  // Área administrativa de produtos: controla formulário, imagem, prévia e visibilidade.
  const productForm = document.querySelector("#admin-product-form");
  const editingProductId = productForm?.elements.productId?.value || null;
  const editingProduct = estado.products.find(
    (item) => item.id === Number(editingProductId),
  );
  const productFormToggle = document.querySelector(
    "[data-toggle-product-form]",
  );
  const imageUrlInput = productForm?.elements.imageUrl;
  const imageFileInput = productForm?.elements.imageFile;
  const previewImage = document.querySelector("#product-image-preview img");
  const previewText = document.querySelector("#product-image-preview span");
  const showProductPreview = (source) => {
    // A prévia troca entre texto vazio e imagem conforme a origem disponível.
    if (!previewImage || !previewText) return;
    previewImage.src = source || "";
    previewImage.hidden = !source;
    previewText.hidden = Boolean(source);
  };
  productFormToggle?.addEventListener("click", () => {
    const willOpen = productForm.classList.contains("hidden-section");
    productForm.classList.toggle("hidden-section");
    productFormToggle.textContent = willOpen
      ? "Fechar cadastro"
      : "Cadastrar novo produto";
    productFormToggle.classList.toggle("btn-outline-secondary", willOpen);
    productFormToggle.classList.toggle("aura-btn", !willOpen);
    if (willOpen) productForm.querySelector('[name="code"]')?.focus();
  });
  imageUrlInput?.addEventListener("input", () =>
    showProductPreview(imageUrlInput.value.trim() || editingProduct?.image || ""),
  );
  imageFileInput?.addEventListener("change", async () => {
    const file = imageFileInput.files?.[0];
    if (!file)
      return showProductPreview(
        imageUrlInput?.value.trim() || editingProduct?.image || "",
      );
    if (!file.type.startsWith("image/") || file.size > 1.5 * 1024 * 1024)
      return exibirToast(
        "Selecione uma imagem PNG, JPG, WEBP ou GIF de até 1,5 MB.",
        "error",
      );
    try {
      // FileReader roda de forma assíncrona; por isso o listener precisa ser async.
      showProductPreview(await lerArquivoImagem(file));
    } catch (error) {
      exibirToast(error.message, "error");
    }
  });
  productForm?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const data = lerDadosFormulario(productForm);
    const file = imageFileInput?.files?.[0];
    if (
      file &&
      (!file.type.startsWith("image/") || file.size > 1.5 * 1024 * 1024)
    )
      return exibirToast("Selecione uma imagem válida de até 1,5 MB.", "error");
    try {
      data.image = file
        ? await lerArquivoImagem(file)
        : String(data.imageUrl || "").trim() || editingProduct?.image || "";
      const item = executarAcao(
        () =>
          editingProductId
            ? editarProduto(editingProductId, {
                ...data,
                visible: Boolean(data.visible),
              })
            : cadastrarProduto({ ...data, visible: Boolean(data.visible) }),
        null,
        false,
      );
      if (item) {
        exibirToast(
          editingProductId
            ? `Produto ${item.name} atualizado.`
            : `Produto ${item.name} cadastrado.`,
        );
        navegar("/admin/produtos");
      }
    } catch (error) {
      exibirToast(error.message, "error");
    }
  });

  // Área administrativa de cupons: só cupons promocionais são editáveis manualmente.
  document
    .querySelectorAll("[data-toggle-product]")
    .forEach((button) =>
      button.addEventListener("click", () =>
        executarAcao(
          () => alternarVisibilidadeProduto(button.dataset.toggleProduct),
          "Visibilidade atualizada.",
        ),
      ),
    );
  const couponForm = document.querySelector("#admin-coupon-form");
  const couponFormToggle = document.querySelector("[data-toggle-coupon-form]");
  couponFormToggle?.addEventListener("click", () => {
    const willOpen = couponForm.classList.contains("hidden-section");
    couponForm.classList.toggle("hidden-section");
    couponFormToggle.textContent = willOpen
      ? "Fechar cadastro"
      : "Cadastrar cupom promocional";
    couponFormToggle.classList.toggle("btn-outline-secondary", willOpen);
    couponFormToggle.classList.toggle("aura-btn", !willOpen);
    if (willOpen) couponForm.querySelector('[name="code"]')?.focus();
  });
  couponForm?.addEventListener("submit", (event) => {
    event.preventDefault();
    const data = lerDadosFormulario(couponForm);
    const editing = data.couponId || null;
    const item = executarAcao(
      () => salvarCupomPromocional(data, editing),
      editing
        ? "Cupom promocional atualizado."
        : "Cupom promocional cadastrado.",
      false,
    );
    if (item) navegar("/admin/cupons");
  });
  document.querySelectorAll("[data-inactivate-promo]").forEach((button) =>
    button.addEventListener("click", () => {
      if (
        !confirm(
          "Inativar este cupom promocional? Ele deixará de ser aceito no checkout.",
        )
      )
        return;
      const item = executarAcao(
        () => inativarCupomPromocional(button.dataset.inactivatePromo),
        "Cupom promocional inativado.",
        false,
      );
      if (item) navegar("/admin/cupons");
    }),
  );
  document.querySelector("[data-reset-demo]")?.addEventListener("click", () => {
    if (!confirm("Restaurar todos os dados locais da demonstração?")) return;
    restaurarDemonstracao();
    exibirToast("Massa demonstrativa restaurada.");
    navegar("/admin/dashboard");
  });

  // Módulos especializados recebem os callbacks necessários sem conhecer o roteador inteiro.
  inicializarAnalytics(navegar);
  inicializarConsultoraAura({
    exibirToast,
    renderizar,
  });
}

window.addEventListener("popstate", renderizar);
renderizar();
