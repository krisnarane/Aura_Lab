import assert from 'node:assert/strict';
import { produtosApi } from '../js/produtosApi.js';

const originalFetch = globalThis.fetch;
try {
  globalThis.fetch = async () => { throw new TypeError('Network error'); };
  await assert.rejects(produtosApi.listar(), e => e.codigo === 'REDE_INDISPONIVEL');
  globalThis.fetch = async () => new Response(JSON.stringify({codigo:'PRODUTO_JA_ATIVO',mensagem:'Este produto já está ativo.',campos:{}}), {status:409});
  await assert.rejects(produtosApi.ativar(1, {}), e => e.codigo === 'PRODUTO_JA_ATIVO');
  globalThis.fetch = async url => {
    const parsed = new URL(url,'http://localhost');
    assert.equal(parsed.searchParams.get('nome'),'vitamina');
    assert.equal(parsed.searchParams.get('ativo'),'false');
    return new Response('[]',{status:200});
  };
  assert.deepEqual(await produtosApi.listar({nome:'vitamina',ativo:false}),[]);
  let corpo = null;
  globalThis.fetch = async (url, opcoes) => {
    assert.equal(url,'/api/v1/produtos/3/ativacao');
    assert.equal(opcoes.method,'PATCH');
    corpo = JSON.parse(opcoes.body);
    return new Response(JSON.stringify({id:3,ativo:true,categoriaAtivacao:corpo.categoria}),{status:200});
  };
  const ativado = await produtosApi.ativar(3,{categoria:'RETORNO_AO_MERCADO',justificativa:'Novo lote.'});
  assert.deepEqual(corpo,{categoria:'RETORNO_AO_MERCADO',justificativa:'Novo lote.'});
  assert.equal(ativado.ativo,true);
  globalThis.fetch = async () => new Response(JSON.stringify([{codigo:'RETORNO_AO_MERCADO',descricao:'Retorno ao mercado'}]),{status:200});
  assert.equal((await produtosApi.categoriasAtivacao())[0].codigo,'RETORNO_AO_MERCADO');
  console.log('Adapter de produtos: erros, filtros, motivo de ativação e domínios validados.');
} finally { globalThis.fetch = originalFetch; }
