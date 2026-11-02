/** Contrato HTTP do chat RNF0044. */
async function requisitar(caminho, opcoes = {}) {
  let resposta;
  try {
    resposta = await fetch(`/api/v1${caminho}`, {
      ...opcoes,
      headers: { "Content-Type": "application/json", ...(opcoes.headers || {}) },
    });
  } catch { throw Error("Não foi possível conectar ao servidor."); }
  if (!resposta.ok) {
    const erro = await resposta.json().catch(() => ({}));
    throw Error(erro.mensagem || "Não foi possível concluir a operação.");
  }
  return resposta.json();
}

export const consultoraApi = {
  conversas: (cliente) => requisitar(`/clientes/${cliente}/chat/conversas`),
  mensagens: (cliente, conversa) => requisitar(`/clientes/${cliente}/chat/conversas/${conversa}`),
  enviar: (cliente, conversa, texto) => requisitar(
    `/clientes/${cliente}/chat/conversas${conversa ? `/${conversa}` : ""}/mensagens`,
    { method: "POST", body: JSON.stringify({ texto }) },
  ),
  avaliar: (cliente, resposta, util, motivo) => requisitar(
    `/clientes/${cliente}/chat/respostas/${resposta}/avaliacao`,
    { method: "PUT", body: JSON.stringify({ util, motivo }) },
  ),
};
