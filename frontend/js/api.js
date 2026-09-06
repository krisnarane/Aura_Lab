/**
 * Único ponto de integração HTTP do protótipo. A aplicação continua
 * demonstrável sem back-end: chamadas indisponíveis retornam null e a tela
 * usa a recomendação local baseada nas propriedades do catálogo.
 */
const BASE_API = "/api/v1";

export async function perguntarConsultoraAura(mensagem) {
  try {
    // A tentativa HTTP é opcional: se a API ainda não existir, o front usa a regra local.
    const resposta = await fetch(`${BASE_API}/beauty-advisor/recommendations`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      // "message" permanece em inglês porque fará parte do contrato da futura API REST.
      body: JSON.stringify({ message: mensagem }),
    });
    if (!resposta.ok) return null;
    return await resposta.json();
  } catch {
    // Falhas de rede não quebram a experiência demonstrativa.
    return null;
  }
}

// Alias de compatibilidade para integrações que ainda usam o nome anterior.
export { perguntarConsultoraAura as askBeautyAdvisor };
