import { escaparHtml as e } from "./utilitarios.js";
export function prepararFormularios(raiz) {
  raiz.querySelectorAll("form").forEach((form) => {
    form.noValidate = true;
    form.querySelectorAll("input,select,textarea").forEach((c) => {
      const id = `${form.dataset.testid || "form"}-${c.name}`;
      c.id = id;
      c.dataset.testid ||= id;
      if (!c.closest("label"))
        c.setAttribute("aria-label", c.placeholder || c.name);
    });
  });
}
export async function executar(form, acao, toast, apos) {
  if (form.dataset.enviando) return;
  form.dataset.enviando = "true";
  const botoes = [...form.querySelectorAll("button")];
  botoes.forEach((b) => (b.disabled = true));
  form
    .querySelectorAll("[data-form-error],[data-field-error]")
    .forEach((e) => e.remove());
  form.querySelectorAll("[aria-invalid]").forEach((e) => {
    e.removeAttribute("aria-invalid");
    e.removeAttribute("aria-describedby");
  });
  try {
    const pendente = acao();
    form
      .querySelectorAll('input[type="password"], [name="numero"]')
      .forEach((c) => {
        if (c.type === "password" || form.dataset.testid === "form-cartao")
          c.value = "";
      });
    await pendente;
    if (!form.isConnected) return;
    toast("Operação concluída.");
    await apos();
  } catch (err) {
    if (!form.isConnected) return;
    form.insertAdjacentHTML(
      "afterbegin",
      `<div data-form-error data-testid="erro-api" class="alert alert-danger" role="alert">${e(err.message)}</div>`,
    );
    for (const [nome, mensagem] of Object.entries(err.campos || {})) {
      const normal = nome.replace("enderecoResidencial.", "endereco_");
      const campo =
        form.elements.namedItem(normal) ||
        form.elements.namedItem(`endereco_${nome}`) ||
        (nome === "senha" ? form.elements.namedItem("novaSenha") : null);
      if (campo) {
        const id = `${campo.id}-erro`;
        campo.setAttribute("aria-invalid", "true");
        campo.setAttribute("aria-describedby", id);
        campo.insertAdjacentHTML(
          "afterend",
          `<small id="${e(id)}" data-field-error class="text-danger">${e(mensagem)}</small>`,
        );
      }
    }
    form.querySelector('[aria-invalid="true"]')?.focus();
  } finally {
    delete form.dataset.enviando;
    botoes.forEach((b) => (b.disabled = false));
  }
}
export async function executarBotao(botao, acao, toast, apos) {
  if (botao.disabled) return;
  botao.disabled = true;
  try {
    await acao();
    if (!botao.isConnected) return;
    toast("Operação concluída.");
    await apos();
  } catch (err) {
    toast(err.message, "error");
  } finally {
    botao.disabled = false;
  }
}
