# Requisitos do módulo de produtos

Fonte disponível: `docs/DRS_LES_2_2026.docx` (grupo **Cadastro de Livros**). No Aura Lab, **livro do DRS = produto da loja**. Este resumo serve para rastreabilidade; em caso de dúvida, consulte a tabela original do DRS.

O módulo integrado cobre cadastro, edição e consulta dos campos do Aura Lab, além do ciclo ativar/inativar com motivo. Campos editoriais exclusivos do livro no DRS permanecem fora do escopo definido para este incremento.

## Requisitos funcionais — status

| ID | Nome do DRS | Cobertura atual |
| --- | --- | --- |
| RF0011 | Cadastrar produto (livro) | **Implementado para o Aura Lab**: nome, marca, descrição, URL de imagem, preço, estoque, visibilidade e múltiplas categorias; código `PRD-` gerado na gravação. Campos editoriais exclusivos de livros não fazem parte deste incremento |
| RF0012 | Inativar cadastro de produto | **Implementado**: inativação lógica com motivo obrigatório (categoria + justificativa); inativação força `visivel = false` |
| RF0013 | Inativar produto de forma automática | **Não coberto**: exige dados de estoque e vendas/parâmetro; fora da entrega |
| RF0014 | Alterar cadastro de produto | **Implementado para os campos do Aura Lab**: edição preserva código, situação e motivos; estoque não pode ficar abaixo das reservas vigentes |
| RF0015 | Consulta de produtos | **Implementado para os filtros deste incremento**: código, nome, marca, categoria e situação combináveis |
| RF0016 | Ativar cadastro de produto (livro) | **Implementado**: ativação lógica com motivo obrigatório (RN0017); não republica automaticamente (`visivel` permanece como estava) |

## Regras de negócio — status

| ID | Regra do DRS | Aplicação atual |
| --- | --- | --- |
| RN0011 | Dados obrigatórios do livro (autor, categorias, ano, ISBN, dimensões, grupo de precificação...) | **Parcial**: o cadastro mínimo usa nome, marca, preço, estoque; os demais campos aguardam a modelagem completa do DVP |
| RN0012 | Livro pode ter mais de uma categoria | **Implementado**: produto pode ter múltiplas categorias da lista controlada |
| RN0013 | Valor de venda por grupo de precificação | **Não coberto** |
| RN0014 | Alteração de preço dentro da margem | **Não coberto** |
| RN0015 | Justificativa + categoria de inativação manual | **Implementado**: `categoria_inativacao_produto` seeded; 400 `CATEGORIA_PRODUTO_INVALIDA`, justificativa obrigatória |
| RN0016 | Inativação automática = FORA DE MERCADO | Vinculada ao RF0013, também não coberto |
| RN0017 | Justificativa + categoria de ativação | **Implementado**: `categoria_ativacao_produto` seeded (RETORNO_AO_MERCADO, REPOSICAO_ESTOQUE, CORRECAO_CADASTRO); mesma validação de inativação |

## Requisitos não funcionais — status

| ID | Exigência | Cobertura |
| --- | --- | --- |
| RNF0012 | Log de toda escrita (data, hora, ator, dados) | `auditoria_produto` na mesma transação, ator técnico `ADMIN_DEMO`, snapshots de cadastro e edição antes/depois, situação e motivo |
| RNF0013 | Tabelas de domínio via script de implantação | Categorias de produto nascem na V11; motivos de ativação/inativação na V5 |
| RNF0021 | Código único de livro | Trigger `atribuir_codigo_produto()` gera `PRD-######` único e imutável, espelhando o de clientes |

## Contrato e erros

Endpoints em `/api/v1`: `GET/POST /produtos`, `GET/PATCH /produtos/{id}`, `PATCH /produtos/{id}/inativacao`, `PATCH /produtos/{id}/ativacao` (corpo `{categoria, justificativa}`), `GET /categorias-produto`, `GET /categorias-ativacao-produto`, `GET /categorias-inativacao-produto`.

Erros seguem `{codigo, mensagem, campos}`: 400 `CATEGORIA_PRODUTO_INVALIDA` e validações de campos, 404 `PRODUTO_NAO_ENCONTRADO`, 409 `PRODUTO_JA_ATIVO` / `PRODUTO_JA_INATIVO`. Tentativa inválida não gera auditoria.

## Convivência com o protótipo

- `/admin/produtos` agora é tela **integrada** (interceptada em `app.js`, com Adapter `produtosApi.js` e Facade `produtosService.js`); a versão de `localStorage` da mesma rota fica inativa.
- Catálogo e detalhe da loja leem os produtos do PostgreSQL e exibem descrição, imagem e categorias. A arte colorida permanece quando a imagem externa falha.

## Evidência Selenium

Cenários em `backend/src/test/java/br/com/auralab/e2e/ProdutosCrudIT.java`: `RF0016_RN0017_RNF0012_ativacaoCancelarConfirmarRepetir` (espelha o padrão de RF0023), `RF0012_RN0015_RNF0012_inativacaoRegistraMotivoEAuditoria`, `RN0017_justificativaObrigatoriaNaoAtiva` e `RF0011_RNF0021_cadastroGeraCodigoEApareceNaLista`. Testes de apoio: `ProdutoApiTest` e `MigrationPersistenceTest` (reinício preserva situação e motivo).
