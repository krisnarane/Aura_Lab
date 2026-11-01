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
  globalThis.fetch = async url => {
    const parsed = new URL(url,'http://localhost');
    assert.equal(parsed.searchParams.get('marca'),'Aura Derma');
    assert.equal(parsed.searchParams.get('categoria'),'FACE');
    return new Response('[]',{status:200});
  };
  assert.deepEqual(await produtosApi.listar({marca:'Aura Derma',categoria:'FACE'}),[]);
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
  globalThis.fetch = async (url, opcoes) => {
    assert.equal(url, '/api/v1/produtos/4');
    assert.equal(opcoes.method, 'PATCH');
    assert.deepEqual(JSON.parse(opcoes.body).categorias, ['FACE', 'SKINCARE']);
    return new Response(JSON.stringify({id:4,categorias:[{codigo:'FACE'},{codigo:'SKINCARE'}]}),{status:200});
  };
  assert.equal((await produtosApi.editar(4,{categorias:['FACE','SKINCARE']})).categorias.length,2);
  globalThis.fetch = async (url) => {
    assert.equal(url, '/api/v1/categorias-produto');
    return new Response(JSON.stringify([{codigo:'FACE',descricao:'Face'}]),{status:200});
  };
  assert.equal((await produtosApi.categoriasProduto())[0].codigo,'FACE');
  const chamadas = [];
  globalThis.fetch = async (url, opcoes = {}) => {
    chamadas.push([url, opcoes.method || 'GET', opcoes.body && JSON.parse(opcoes.body)]);
    return new Response(url.endsWith('inativacao-automatica') ? '[1]' : '[]', {status:200});
  };
  await produtosApi.gruposPrecificacao();
  await produtosApi.entradas(1);
  await produtosApi.registrarEntrada(1, {quantidade:2,valorCusto:10,dataEntrada:'2026-10-12'});
  await produtosApi.inativacaoAutomatica();
  assert.deepEqual(chamadas.map(c => c.slice(0, 2)), [
    ['/api/v1/grupos-precificacao','GET'],
    ['/api/v1/produtos/1/entradas-estoque','GET'],
    ['/api/v1/produtos/1/entradas-estoque','POST'],
    ['/api/v1/produtos/inativacao-automatica','POST']]);
  assert.equal(chamadas[2][2].quantidade, 2);
  console.log('Adapter de produtos: erros, filtros, motivo de ativação e domínios validados.');
} finally { globalThis.fetch = originalFetch; }
