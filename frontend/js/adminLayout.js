export const criarNavegacaoAdmin = (active, atributo = "data-link") =>
  `<aside class="admin-side"><a href="/admin/dashboard" ${atributo} class="brand light">aura <i>LAB</i></a><small>GESTÃO</small>${[
    ["Dashboard", "/admin/dashboard", "Dashboard"],
    ["Clientes", "/admin/clientes", "Clientes"],
    ["Pedidos", "/admin/pedidos", "Pedidos"],
    ["Trocas", "/admin/trocas", "Trocas"],
    ["Produtos", "/admin/produtos", "Produtos"],
    ["Cupons", "/admin/cupons", "Cupons"],
    ["Configurações", "/admin/configuracoes", "Configurações"],
  ]
    .map(
      ([label, href, key]) =>
        `<a ${atributo} href="${href}" class="${active === key ? "active" : ""}">${label}</a>`,
    )
    .join("")}<a href="/" ${atributo} class="admin-store-link">← Voltar para a loja</a></aside>`;
