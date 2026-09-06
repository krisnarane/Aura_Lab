/**
 * Aplica detalhes visuais que dependem dos dados após cada renderização.
 * Assim, os templates não precisam de estilos ou eventos inline.
 */
export function aplicarDetalhesVisuais(raiz = document) {
  // Coleta as cores declaradas nos cards para gerar variáveis CSS dinâmicas.
  const coresProdutos = [
    ...new Set(
      Array.from(raiz.querySelectorAll("[data-cor-produto]")).map(
        (elemento) => elemento.dataset.corProduto,
      ),
    ),
  ];

  // Mantém uma única tag <style>, atualizada a cada nova renderização da SPA.
  let folhaCores = document.querySelector("#cores-produtos-dinamicas");
  if (!folhaCores) {
    folhaCores = document.createElement("style");
    folhaCores.id = "cores-produtos-dinamicas";
    document.head.append(folhaCores);
  }
  folhaCores.textContent = coresProdutos
    .map(
      (cor) =>
        `[data-cor-produto="${CSS.escape(cor)}"] { --art: ${cor}; }`,
    )
    .join("\n");

  // Se uma imagem cadastrada quebrar, o card volta para a arte de cor sem deixar ícone quebrado.
  raiz.querySelectorAll("[data-ocultar-imagem-com-erro]").forEach((imagem) => {
    imagem.addEventListener(
      "error",
      () => {
        imagem.hidden = true;
      },
      { once: true },
    );
  });
}
