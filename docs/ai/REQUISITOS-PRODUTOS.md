# Requisitos do módulo de produtos

Fonte disponível: `docs/DRS_LES_2_2026.docx` (grupo **Cadastro de Livros**). No Aura Lab, **livro do DRS = produto da loja**. Este resumo serve para rastreabilidade; em caso de dúvida, consulte a tabela original do DRS.

O módulo integrado cobre cadastro, edição e consulta dos campos do Aura Lab, além do ciclo ativar/inativar com motivo. Campos editoriais exclusivos do livro no DRS permanecem fora do escopo definido para este incremento.

## Requisitos funcionais — status

| ID | Nome do DRS | Cobertura atual |
| --- | --- | --- |
| RF0011 | Cadastrar produto (livro) | **Implementado para o Aura Lab**: nome, marca, descrição, URL de imagem, preço, estoque, visibilidade e múltiplas categorias; código `PRD-` gerado na gravação. Campos editoriais exclusivos de livros não fazem parte deste incremento |
| RF0012 | Inativar cadastro de produto | **Implementado**: inativação lógica com motivo obrigatório (categoria + justificativa); inativação força `visivel = false` |
| RF0013 | Inativar produto de forma automática | **Implementado**: job e execução administrativa explícita inativam produto ativo com estoque zero, nunca vendido e preço abaixo do parâmetro `aura.produtos.preco-limite-inativacao` (padrão R$ 50,00) |
| RF0014 | Alterar cadastro de produto | **Implementado para os campos do Aura Lab**: edição preserva código, situação e motivos; estoque não pode ficar abaixo das reservas vigentes |
| RF0015 | Consulta de produtos | **Implementado**: código, nome, marca, descrição, categoria, grupo, faixas de preço/custo/estoque, situação e visibilidade combináveis |
| RF0016 | Ativar cadastro de produto (livro) | **Implementado**: ativação lógica com motivo obrigatório (RN0017); não republica automaticamente (`visivel` permanece como estava) |
| RF0051 | Entrada em estoque | **Implementado**: quantidade, custo e data, com histórico persistido e auditoria transacional |
| RF0052 | Precificação da entrada | **Implementado**: toda entrada recalcula preço por custo × (1 + margem do grupo); preço continua editável no cadastro/edição |

## Regras de negócio — status

| ID | Regra do DRS | Aplicação atual |
| --- | --- | --- |
| RN0011 | Dados obrigatórios do livro (autor, categorias, ano, ISBN, dimensões, grupo de precificação...) | **Parcial**: o cadastro do Aura Lab exige nome, marca, preço, estoque e grupo de precificação; os campos editoriais aguardam modelagem própria |
| RN0012 | Livro pode ter mais de uma categoria | **Implementado**: produto pode ter múltiplas categorias da lista controlada |
| RN0013 | Valor de venda por grupo de precificação | **Implementado na entrada**: grupo obrigatório no produto; `PADRAO`, `PREMIUM` e `BASICO` são semeados pela V13 |
| RN0014 | Alteração de preço dentro da margem | **Não coberto** |
| RN0015 | Justificativa + categoria de inativação manual | **Implementado**: `categoria_inativacao_produto` seeded; 400 `CATEGORIA_PRODUTO_INVALIDA`, justificativa obrigatória |
| RN0016 | Inativação automática = FORA DE MERCADO | **Implementado**: categoria `FORA_DE_MERCADO`, justificativa automática e ator técnico `SISTEMA` |
| RN0050 | Dados da entrada | Produto, quantidade, custo e data; sem fornecedor por decisão deste incremento |
| RN0061/RN0062/RNF0064 | Validações da entrada | Quantidade e custo positivos; data obrigatória, também protegidos por checks no banco |
| RN0017 | Justificativa + categoria de ativação | **Implementado**: `categoria_ativacao_produto` seeded (RETORNO_AO_MERCADO, REPOSICAO_ESTOQUE, CORRECAO_CADASTRO); mesma validação de inativação |

## Requisitos não funcionais — status

| ID | Exigência | Cobertura |
| --- | --- | --- |
| RNF0012 | Log de toda escrita (data, hora, ator, dados) | `auditoria_produto` na mesma transação, ator técnico `ADMIN_DEMO`, snapshots de cadastro e edição antes/depois, situação e motivo |
| RNF0013 | Tabelas de domínio via script de implantação | Categorias de produto nascem na V11; motivos na V5; grupos de precificação na V13 |
| RNF0021 | Código único de livro | Trigger `atribuir_codigo_produto()` gera `PRD-######` único e imutável, espelhando o de clientes |

## Contrato e erros

Endpoints em `/api/v1`: `GET/POST /produtos`, `GET/PATCH /produtos/{id}`, `PATCH /produtos/{id}/inativacao`, `PATCH /produtos/{id}/ativacao` (corpo `{categoria, justificativa}`), `GET /categorias-produto`, `GET /categorias-ativacao-produto`, `GET /categorias-inativacao-produto`.

O cadastro e a edição exigem `grupoPrecificacao`. A API também expõe `GET /grupos-precificacao`, `POST/GET /produtos/{id}/entradas-estoque` e `POST /produtos/inativacao-automatica` (retorna IDs inativados). Produto inativo pode receber entrada, sem reativação automática. "Nunca vendido" significa ausência de item de pedido em status diferente de `EM_PROCESSAMENTO` e `REPROVADA`; uma compra trocada já foi vendida. O job é desligado nos testes via `aura.produtos.agendamento-inativacao-ativo: false`.

Erros seguem `{codigo, mensagem, campos}`: 400 `CATEGORIA_PRODUTO_INVALIDA` e validações de campos, 404 `PRODUTO_NAO_ENCONTRADO`, 409 `PRODUTO_JA_ATIVO` / `PRODUTO_JA_INATIVO`. Tentativa inválida não gera auditoria.

## Convivência com o protótipo

- `/admin/produtos` agora é tela **integrada** (interceptada em `app.js`, com Adapter `produtosApi.js` e Facade `produtosService.js`); a versão de `localStorage` da mesma rota fica inativa.
- Catálogo e detalhe da loja leem os produtos do PostgreSQL e exibem descrição, imagem e categorias. A arte colorida permanece quando a imagem externa falha.

## Evidência Selenium

Cenários em `backend/src/test/java/br/com/auralab/e2e/ProdutosCrudIT.java`: `RF0016_RN0017_RNF0012_ativacaoCancelarConfirmarRepetir` (espelha o padrão de RF0023), `RF0012_RN0015_RNF0012_inativacaoRegistraMotivoEAuditoria`, `RN0017_justificativaObrigatoriaNaoAtiva` e `RF0011_RNF0021_cadastroGeraCodigoEApareceNaLista`. Testes de apoio: `ProdutoApiTest` e `MigrationPersistenceTest` (reinício preserva situação e motivo).
