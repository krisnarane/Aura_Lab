# Requisitos do módulo de produtos

Fonte disponível: `docs/DRS_LES_2_2026.docx` (grupo **Cadastro de Livros**). No Aura Lab, **livro do DRS = produto da loja**. Este resumo serve para rastreabilidade; em caso de dúvida, consulte a tabela original do DRS.

O módulo integrado foi criado para cobrir com fidelidade o ciclo ativar/inativar com motivo. Cadastro e consulta existem no mínimo necessário para suportar esse ciclo; os itens marcados como parciais ou não cobertos estão declarados assim de propósito — não fingir aderência.

## Requisitos funcionais — status

| ID | Nome do DRS | Cobertura atual |
| --- | --- | --- |
| RF0011 | Cadastrar produto (livro) | **Parcial**: nome, marca, preço, estoque inicial e visibilidade; código `PRD-` gerado na gravação. Campos editoriais completos de RN0011 não fazem parte desta entrega |
| RF0012 | Inativar cadastro de produto | **Implementado**: inativação lógica com motivo obrigatório (categoria + justificativa); inativação força `visivel = false` |
| RF0013 | Inativar produto de forma automática | **Não coberto**: exige dados de estoque e vendas/parâmetro; fora da entrega |
| RF0014 | Alterar cadastro de produto | **Não coberto** nesta entrega |
| RF0015 | Consulta de produtos | **Parcial**: filtros de código, nome (parcial, `%`/`_` literais) e situação, combináveis |
| RF0016 | Ativar cadastro de produto (livro) | **Implementado**: ativação lógica com motivo obrigatório (RN0017); não republica automaticamente (`visivel` permanece como estava) |

## Regras de negócio — status

| ID | Regra do DRS | Aplicação atual |
| --- | --- | --- |
| RN0011 | Dados obrigatórios do livro (autor, categorias, ano, ISBN, dimensões, grupo de precificação...) | **Parcial**: o cadastro mínimo usa nome, marca, preço, estoque; os demais campos aguardam a modelagem completa do DVP |
| RN0012 | Livro pode ter mais de uma categoria | **Não coberto** |
| RN0013 | Valor de venda por grupo de precificação | **Não coberto** |
| RN0014 | Alteração de preço dentro da margem | **Não coberto** |
| RN0015 | Justificativa + categoria de inativação manual | **Implementado**: `categoria_inativacao_produto` seeded; 400 `CATEGORIA_PRODUTO_INVALIDA`, justificativa obrigatória |
| RN0016 | Inativação automática = FORA DE MERCADO | Vinculada ao RF0013, também não coberto |
| RN0017 | Justificativa + categoria de ativação | **Implementado**: `categoria_ativacao_produto` seeded (RETORNO_AO_MERCADO, REPOSICAO_ESTOQUE, CORRECAO_CADASTRO); mesma validação de inativação |

## Requisitos não funcionais — status

| ID | Exigência | Cobertura |
| --- | --- | --- |
| RNF0012 | Log de toda escrita (data, hora, ator, dados) | `auditoria_produto` na mesma transação, ator técnico `ADMIN_DEMO`, snapshots com situação e motivo antes/depois |
| RNF0013 | Tabelas de domínio via script de implantação | Parcial para produtos: categorias de ativação/inativação nascem nas migrações V5 e são resemeadas nos testes |
| RNF0021 | Código único de livro | Trigger `atribuir_codigo_produto()` gera `PRD-######` único e imutável, espelhando o de clientes |

## Contrato e erros

Endpoints em `/api/v1`: `GET/POST /produtos`, `GET /produtos/{id}`, `PATCH /produtos/{id}/inativacao`, `PATCH /produtos/{id}/ativacao` (corpo `{categoria, justificativa}`), `GET /categorias-ativacao-produto`, `GET /categorias-inativacao-produto`.

Erros seguem `{codigo, mensagem, campos}`: 400 `CATEGORIA_PRODUTO_INVALIDA` e validações de campos, 404 `PRODUTO_NAO_ENCONTRADO`, 409 `PRODUTO_JA_ATIVO` / `PRODUTO_JA_INATIVO`. Tentativa inválida não gera auditoria.

## Convivência com o protótipo

- `/admin/produtos` agora é tela **integrada** (interceptada em `app.js`, com Adapter `produtosApi.js` e Facade `produtosService.js`); a versão de `localStorage` da mesma rota fica inativa.
- As jornadas da loja (catálogo, sacola) continuam lendo o store do protótipo: produtos do backend ainda não aparecem para o cliente final. Divergência conhecida até a integração do catálogo no DVP.

## Evidência Selenium

Cenários em `backend/src/test/java/br/com/auralab/e2e/ProdutosCrudIT.java`: `RF0016_RN0017_RNF0012_ativacaoCancelarConfirmarRepetir` (espelha o padrão de RF0023), `RF0012_RN0015_RNF0012_inativacaoRegistraMotivoEAuditoria`, `RN0017_justificativaObrigatoriaNaoAtiva` e `RF0011_RNF0021_cadastroGeraCodigoEApareceNaLista`. Testes de apoio: `ProdutoApiTest` e `MigrationPersistenceTest` (reinício preserva situação e motivo).
