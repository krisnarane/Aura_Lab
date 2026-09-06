/**
 * Controla a conversa da Consultora Aura e sua recomendação local por regras.
 * A consultora apenas recomenda itens do catálogo; nunca altera preço ou regra transacional.
 */
import { add, product } from "./store.js";
import { escaparHtml, formatarMoeda } from "./utilitarios.js";
import { aplicarDetalhesVisuais } from "./interface.js";

const REGRAS_RECOMENDACAO = [
  { termos: ["oleosa", "brilho"], idsProdutos: [2, 6] },
  { termos: ["sensível", "hidratação"], idsProdutos: [2, 6] },
  { termos: ["base", "mista", "cobertura"], idsProdutos: [1, 3] },
  { termos: ["olhos", "máscara"], idsProdutos: [4, 7] },
  { termos: ["lábios", "batom"], idsProdutos: [5, 8] },
];

function criarCartaoRecomendacao(idProduto) {
  // Busca o produto pelo id e transforma seus dados em um card reutilizável no chat.
  const produto = product(idProduto);
  if (!produto) return "";

  // A imagem é opcional; quando não existe, a arte colorida do produto continua aparecendo.
  const imagem = produto.image
    ? `<img src="${escaparHtml(produto.image)}" alt="${escaparHtml(produto.name)}" data-ocultar-imagem-com-erro>`
    : "";

  return `<article class="aura-card chat-card" data-product-card="${escaparHtml(produto.id)}">
    <div class="product-art" data-cor-produto="${escaparHtml(produto.art || "#b65d78")}">
      ${imagem}<span class="product-mark">aura</span>
    </div>
    <div class="card-body">
      <small>${escaparHtml(produto.brand)}</small>
      <b>${escaparHtml(produto.name)}</b>
      <div class="d-flex align-items-center">
        <b>${formatarMoeda(produto.price)}</b>
        <button class="btn btn-outline-secondary ms-auto" data-add-chat="${escaparHtml(produto.id)}">Adicionar</button>
      </div>
    </div>
  </article>`;
}

function responderConsultoraAura(exibirToast, renderizar) {
  // Captura o texto digitado e a área onde novas mensagens serão inseridas.
  const campoMensagem = document.querySelector("#advisor-message");
  const areaMensagens = document.querySelector("#aura-chat-messages");
  const mensagem = campoMensagem?.value.trim();
  if (!mensagem || !areaMensagens) return;

  // Procura a primeira regra que contenha algum termo citado pela cliente.
  const mensagemNormalizada = mensagem.toLowerCase();
  const regra = REGRAS_RECOMENDACAO.find((item) =>
    item.termos.some((termo) => mensagemNormalizada.includes(termo)),
  );

  // Sempre registra a pergunta antes de mostrar a resposta da consultora.
  areaMensagens.insertAdjacentHTML(
    "beforeend",
    `<div class="chat-message user-message"><b>Você</b><p>${escaparHtml(mensagem)}</p></div>`,
  );

  if (regra) {
    // Quando há regra compatível, a resposta cita os produtos e exibe botões de compra.
    const nomesProdutos = regra.idsProdutos
      .map((idProduto) => product(idProduto)?.name)
      .filter(Boolean)
      .join(" e ");
    const resposta = `Eu recomendaria ${nomesProdutos}. Posso adaptar pelo acabamento que você prefere.`;
    areaMensagens.insertAdjacentHTML(
      "beforeend",
      `<div class="chat-message aura-message"><b>Aura</b><p>${escaparHtml(resposta)}</p></div>`,
    );

    const cartoes = regra.idsProdutos.map(criarCartaoRecomendacao).join("");
    areaMensagens.insertAdjacentHTML(
      "beforeend",
      `<div class="chat-message aura-message"><div class="chat-recommendations">${cartoes}</div></div>`,
    );
    aplicarDetalhesVisuais(areaMensagens);

    const botoesRecentes = Array.from(
      areaMensagens.querySelectorAll("[data-add-chat]"),
    ).slice(-regra.idsProdutos.length);

    // Os botões recém-criados precisam receber eventos depois da inserção do HTML.
    botoesRecentes.forEach((botao) =>
      botao.addEventListener("click", () => {
        const foiAdicionado = add(botao.dataset.addChat, 1);
        exibirToast(
          foiAdicionado
            ? "Produto reservado por 20 minutos."
            : "Quantidade indisponível.",
          foiAdicionado ? "info" : "error",
        );
        if (foiAdicionado) renderizar();
      }),
    );
  } else {
    // Sem uma regra clara, a consultora pede mais contexto em vez de inventar produto.
    const resposta =
      "Conte seu tipo de pele, objetivo ou acabamento desejado para eu recomendar melhor.";
    areaMensagens.insertAdjacentHTML(
      "beforeend",
      `<div class="chat-message aura-message"><b>Aura</b><p>${escaparHtml(resposta)}</p></div>`,
    );
  }

  // Limpa o campo e rola o chat para a última mensagem.
  campoMensagem.value = "";
  areaMensagens.scrollTop = areaMensagens.scrollHeight;
}

export function inicializarConsultoraAura({ exibirToast, renderizar }) {
  // Centraliza o envio para clique no botão e tecla Enter usarem a mesma lógica.
  const responder = () => responderConsultoraAura(exibirToast, renderizar);
  document.querySelector("#advisor-send")?.addEventListener("click", responder);
  document
    .querySelector("#advisor-message")
    ?.addEventListener("keydown", (evento) => {
      if (evento.key === "Enter") {
        evento.preventDefault();
        responder();
      }
    });
}
