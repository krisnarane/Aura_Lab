/** Inicializa filtros, exportação CSV e interação acessível do gráfico administrativo. */
import { salesAnalysis } from "./store.js";

export function inicializarAnalytics(navegar) {
  // Guarda referências dos elementos que existem apenas na página administrativa.
  const form = document.querySelector("#analytics-filter");
  const feedback = document.querySelector("#analytics-filter-feedback");

  // Lê o formulário sempre no momento da ação, garantindo filtros atualizados.
  const filters = () => {
    const data = new FormData(form);
    return {
      startDate: data.get("start"),
      endDate: data.get("end"),
      categories: data.getAll("category"),
    };
  };

  // Valida a seleção antes de navegar ou gerar o arquivo CSV.
  const validate = () => {
    const values = filters();
    if (!values.categories.length)
      throw Error("Selecione ao menos uma categoria para comparar.");
    return salesAnalysis(values);
  };

  // Mostra mensagens de erro sem interromper o restante da tela.
  const showError = (message) => {
    if (!feedback) return;
    feedback.textContent = message;
    feedback.hidden = false;
  };

  // Ao aplicar filtros, a própria URL guarda o período e as categorias escolhidas.
  form?.addEventListener("submit", (event) => {
    event.preventDefault();
    try {
      validate();
      const values = filters();
      const query = new URLSearchParams({
        start: values.startDate,
        end: values.endDate,
      });
      values.categories.forEach((category) => query.append("category", category));
      navegar(`/admin/dashboard?${query.toString()}`);
    } catch (error) {
      showError(error.message);
    }
  });

  // Monta um CSV simples em memória e força o download pelo navegador.
  document.querySelector("#analytics-export")?.addEventListener("click", () => {
    try {
      const analysis = validate();
      const rows = [["Período", "Categoria", "Valor de vendas"]];
      analysis.series.forEach((series) =>
        series.values.forEach((value, index) =>
          rows.push([
            analysis.periods[index].label,
            series.category,
            Number(value).toFixed(2).replace(".", ","),
          ]),
        ),
      );
      const csv = "\uFEFF" + rows.map((row) => row.join(";")).join("\r\n");
      const url = URL.createObjectURL(
        new Blob([csv], { type: "text/csv;charset=utf-8" }),
      );
      const link = document.createElement("a");
      link.href = url;
      link.download = `aura-lab-vendas-${analysis.startDate}-${analysis.endDate}.csv`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      showError(error.message);
    }
  });

}
