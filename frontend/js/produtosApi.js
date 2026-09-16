/** Adapter: concentra o contrato HTTP do módulo de produtos integrado. */
const BASE = "/api/v1";
async function requisitar(caminho, opcoes = {}) {
  let resposta;
  try {
    resposta = await fetch(BASE + caminho, {
      ...opcoes,
      headers: {
        "Content-Type": "application/json",
        ...(opcoes.headers || {}),
      },
    });
  } catch {
    const e = Error("Não foi possível conectar ao servidor. Tente novamente.");
    e.codigo = "REDE_INDISPONIVEL";
    e.campos = {};
    throw e;
  }
  if (!resposta.ok) {
    let erro = {};
    try {
      erro = await resposta.json();
    } catch {}
    const e = Error(erro.mensagem || "Não foi possível concluir a operação.");
    e.codigo = erro.codigo;
    e.campos = erro.campos || {};
    throw e;
  }
  return resposta.status === 204 ? null : resposta.json();
}
const query = (filtros) => {
  const p = new URLSearchParams();
  Object.entries(filtros).forEach(([k, v]) => {
    if (v !== "" && v !== null && v !== undefined) p.set(k, v);
  });
  return p.toString() ? `?${p}` : "";
};
export const produtosApi = {
  listar: (f = {}) => requisitar(`/produtos${query(f)}`),
  buscar: (id) => requisitar(`/produtos/${id}`),
  cadastrar: (d) =>
    requisitar("/produtos", { method: "POST", body: JSON.stringify(d) }),
  inativar: (id, d) =>
    requisitar(`/produtos/${id}/inativacao`, {
      method: "PATCH",
      body: JSON.stringify(d),
    }),
  ativar: (id, d) =>
    requisitar(`/produtos/${id}/ativacao`, {
      method: "PATCH",
      body: JSON.stringify(d),
    }),
  categoriasAtivacao: () => requisitar("/categorias-ativacao-produto"),
  categoriasInativacao: () => requisitar("/categorias-inativacao-produto"),
};
