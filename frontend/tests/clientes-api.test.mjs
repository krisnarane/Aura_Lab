import assert from 'node:assert/strict';
import { clientesApi } from '../js/clientesApi.js';

const originalFetch = globalThis.fetch;
try {
  globalThis.fetch = async () => { throw new TypeError('Network error'); };
  await assert.rejects(clientesApi.listar(), e => e.codigo === 'REDE_INDISPONIVEL');
  globalThis.fetch = async () => new Response(JSON.stringify({codigo:'DADOS_INVALIDOS',mensagem:'Revise os dados',campos:{nome:'Obrigatório'}}), {status:400});
  await assert.rejects(clientesApi.cadastrar({}), e => e.campos.nome === 'Obrigatório');
  globalThis.fetch = async url => {
    const parsed = new URL(url,'http://localhost');
    assert.equal(parsed.searchParams.get('nome'),'A_%');
    assert.equal(parsed.searchParams.get('ativo'),'false');
    return new Response('[]',{status:200});
  };
  assert.deepEqual(await clientesApi.listar({nome:'A_%',ativo:false}),[]);
  globalThis.fetch = async () => new Response(null,{status:204});
  assert.equal(await clientesApi.alterarSenha(1,{}),null);
  console.log('Adapter de clientes: erros, filtros e respostas sem conteúdo validados.');
} finally { globalThis.fetch = originalFetch; }
