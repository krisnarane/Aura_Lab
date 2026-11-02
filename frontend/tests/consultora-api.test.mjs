import assert from 'node:assert/strict';
import { consultoraApi } from '../js/consultoraApi.js';

const originalFetch = globalThis.fetch;
try {
  const chamadas = [];
  globalThis.fetch = async (url, options) => {
    chamadas.push({ url, options });
    return new Response(JSON.stringify({ ok: true }), { status: 200 });
  };
  await consultoraApi.enviar(7, null, 'Quero uma sugestão');
  await consultoraApi.enviar(7, 9, 'E para os lábios?');
  await consultoraApi.avaliar(7, 11, false, 'Não combinou');
  assert.equal(chamadas[0].url, '/api/v1/clientes/7/chat/conversas/mensagens');
  assert.equal(chamadas[1].url, '/api/v1/clientes/7/chat/conversas/9/mensagens');
  assert.deepEqual(JSON.parse(chamadas[2].options.body), { util: false, motivo: 'Não combinou' });
  globalThis.fetch = async () => new Response(JSON.stringify({ mensagem: 'IA indisponível' }), { status: 503 });
  await assert.rejects(consultoraApi.enviar(7, null, 'Teste'), /IA indisponível/);
  console.log('Adapter RNF0044: conversa, avaliação e indisponibilidade validados.');
} finally {
  globalThis.fetch = originalFetch;
}
