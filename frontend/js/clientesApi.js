/** Adapter: concentra o contrato HTTP do módulo de clientes. */
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
export const clientesApi = {
  listar: (f = {}) => requisitar(`/clientes${query(f)}`),
  buscar: (id) => requisitar(`/clientes/${id}`),
  cadastrar: (d) =>
    requisitar("/clientes", { method: "POST", body: JSON.stringify(d) }),
  alterar: (id, d) =>
    requisitar(`/clientes/${id}`, { method: "PUT", body: JSON.stringify(d) }),
  inativar: (id) =>
    requisitar(`/clientes/${id}/inativacao`, { method: "PATCH" }),
  alterarSenha: (id, d) =>
    requisitar(`/clientes/${id}/senha`, {
      method: "PUT",
      body: JSON.stringify(d),
    }),
  adicionarEndereco: (id, d) =>
    requisitar(`/clientes/${id}/enderecos`, {
      method: "POST",
      body: JSON.stringify(d),
    }),
  alterarEndereco: (id, e, d) =>
    requisitar(`/clientes/${id}/enderecos/${e}`, {
      method: "PUT",
      body: JSON.stringify(d),
    }),
  inativarEndereco: (id, e) =>
    requisitar(`/clientes/${id}/enderecos/${e}/inativacao`, {
      method: "PATCH",
    }),
  adicionarCartao: (id, d) =>
    requisitar(`/clientes/${id}/cartoes`, {
      method: "POST",
      body: JSON.stringify(d),
    }),
  preferirCartao: (id, c) =>
    requisitar(`/clientes/${id}/cartoes/${c}/preferencial`, { method: "PUT" }),
  inativarCartao: (id, c) =>
    requisitar(`/clientes/${id}/cartoes/${c}/inativacao`, { method: "PATCH" }),
  transacoes: (id) => requisitar(`/clientes/${id}/transacoes`),
  auditoria: (id) => requisitar(`/clientes/${id}/auditoria`),
  bandeiras: () => requisitar("/bandeiras"),
};
