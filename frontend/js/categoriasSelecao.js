import { escaparHtml as e } from "./utilitarios.js";

/**
 * Caixas de seleção das categorias de produto, usadas no cadastro/edição (RF0011/RF0014)
 * e no filtro da análise de vendas (RF0055). Substitui a lista de seleção múltipla nativa,
 * que exigia Ctrl para marcar mais de uma categoria e não mostrava as marcadas sem rolagem.
 * Reaproveita o padrão visual `.choice-card` já usado nas opções do checkout.
 */
export const caixasCategorias = (
  categorias,
  selecionadas = [],
  { legenda = "Categorias", testId = "selecao-categorias" } = {},
) =>
  `<fieldset class="campo-categorias" data-testid="${e(testId)}"><legend>${e(legenda)}</legend><div class="choice-grid compact">${categorias
    .map(
      (c) =>
        `<label class="choice-card"><input type="checkbox" name="categorias" value="${e(c.codigo)}" id="${e(testId)}-${e(c.codigo)}" data-testid="categoria-${e(c.codigo)}"${selecionadas.includes(c.codigo) ? " checked" : ""}><span><b>${e(c.descricao)}</b></span></label>`,
    )
    .join("")}</div></fieldset>`;

// Lê a seleção na ordem em que as categorias aparecem na tela.
export const categoriasMarcadas = (form) =>
  [...form.querySelectorAll('input[name="categorias"]:checked')].map((c) => c.value);
