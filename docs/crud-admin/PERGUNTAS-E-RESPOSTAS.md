# Perguntas e respostas — preparação para a apresentação

Guia de estudo com perguntas prováveis do professor e respostas preparadas, organizadas por bloco (do mais provável ao mais "pegadinha"). Baseado no [roteiro de apresentação](ROTEIRO-APRESENTACAO.md), no [estado atual](../ai/ESTADO-ATUAL.md) e nos requisitos de [clientes](../ai/REQUISITOS-CLIENTES.md) e [produtos](../ai/REQUISITOS-PRODUTOS.md).

## 1. Escopo e requisitos

| Pergunta provável | Resposta preparada |
|---|---|
| Quais requisitos foram implementados? | Clientes: RF0021–RF0028 (cadastro, alteração, inativação, consulta, transações, endereços, cartões, senha). Produtos: RF0012 e RF0016 completos (inativar/ativar com motivo), RF0011 e RF0015 parciais (cadastro e consulta mínimos). |
| Livro do DRS equivale a quê no projeto? | A produto da loja. O grupo "Cadastro de Livros" do DRS virou o módulo `/admin/produtos`. |
| Por que não tem ativação de cliente? | O DRS só pede inativação de cliente (RF0023) — inativação lógica, sem exclusão física. Ativação (RF0016) pertence ao grupo de livros e está implementada em produtos. |
| O que ficou fora do escopo de produtos? | RF0013 (inativação automática), RF0014 (alteração), RN0012–RN0014 e os campos editoriais completos de RN0011. Declarado como não coberto de propósito — não fingimos aderência. |
| Cadastro e consulta de produtos são completos? | Não — parciais por escopo: cadastro mínimo (nome, marca, preço, estoque, visibilidade) e filtros por código, nome e situação. O ciclo ativar/inativar com motivo é que era o requisito central. |
| E a RN0028, está implementada? | Não, e não deve ser declarada no CRUD. Ela trata do retorno da operadora e baixa de estoque — faz parte do processo de vendas, não do módulo de clientes. |

## 2. Arquitetura e padrões de projeto

| Pergunta provável | Resposta preparada |
|---|---|
| Qual a arquitetura da aplicação? | Fluxo admin: `HTML/JS → clientesService (Facade) → clientesApi (Adapter) → ClienteController (MVC) → Services → Spring Data (Repository) → PostgreSQL`. |
| Quais padrões (GoF/arquiteturais) foram usados? | Facade e Adapter no frontend (`clientesService.js`, `clientesApi.js`), Strategy no ranking (`PoliticaRankingCliente`), Specification nos filtros (`ClienteSpecifications`), Page Object nos testes Selenium, e MVC + Service Layer + Repository no backend. |
| Onde fica a regra de negócio? | Nos services — controllers só recebem/validam DTOs e delegam; repositories só falam com o banco. Há teste de arquitetura que verifica essas dependências. |
| Por que não existe `SenhaService` ou `BandeiraService`? | Decisão consciente: alteração de senha pertence a `ClienteService` e consulta de bandeiras a `CartaoService`. Um service genérico por tabela inflaria a arquitetura sem ganho real. |
| Como funciona a Strategy de ranking? | `PoliticaRankingPorCompras` calcula `min(5, 1 + floor(total/200))` sobre os totais de compra (somente pedidos pagos/concluídos, sem somar pagamentos de novo). A fórmula é decisão do projeto — a RN0027 exige ranking numérico, sem impor o cálculo. |
| Como a consulta de clientes combina filtros? | Via Specifications combinadas com E: nome e e-mail são parciais (com `%`/`_` tratados como literais), código e os demais campos são igualdade normalizada. |
| Existe autenticação? | Não nesta etapa — decisão de escopo. `ADMIN_DEMO` é só um ator técnico que o servidor grava na auditoria. Não existe `UsuarioAcesso` nem sessão. |

## 3. Dados sensíveis e segurança

| Pergunta provável | Resposta preparada |
|---|---|
| Como a senha é armazenada? | Hash BCrypt (custo 10) em `senha_hash`. Texto puro nunca é persistido, nem aparece em resposta, log ou snapshot de auditoria (RNF0033). |
| O que RNF0031/RNF0032 pedem? | Senha forte: mínimo 8 caracteres com maiúscula, minúscula e especial; mais confirmação idêntica. O limite de 72 bytes vem do próprio algoritmo BCrypt. |
| Por que existe tela de senha sem login? | RF0028 exige alteração isolada da senha — formulário e endpoint próprios, sem editar o restante do cadastro. Autenticação não faz parte desta entrega. |
| O cartão do cliente é salvo inteiro? | Nunca. Número e CVV são validados na requisição (dígitos, Luhn, formato) e descartados; só persistem titular, bandeira e os últimos quatro dígitos (RN0024). Nada vai para a auditoria. |
| A bandeira do cartão pode ser qualquer texto? | Não — deve existir na tabela de domínio `bandeira`, alimentada por migração Flyway (RN0025/RNF0013). |
| Como a auditoria protege segredos? | `RegistroCliente` monta snapshots com campos explicitamente permitidos (allowlist). Troca de senha vira apenas "senha alterada; conteúdo omitido". |

## 4. Banco de dados, transações e regras de integridade

| Pergunta provável | Resposta preparada |
|---|---|
| Qual banco é usado? Por quê? | PostgreSQL 17 — banco real de produção. Testes usam PostgreSQL via Testcontainers; H2 foi descartado porque pode diferir de comportamento do banco real. |
| Como o banco é versionado? | Flyway: V1–V4 criam clientes, endereços, bandeiras, cartões, auditoria e transações demonstrativas; V5 cria produtos e as tabelas de domínio de motivo; V6 traz massa de produtos. Migrações aplicadas são imutáveis — mudança nova entra na próxima versão. |
| De onde vêm os códigos `CLI-` e `PRD-`? | De triggers no banco (`atribuir_codigo_...`), gerando código único e imutável na gravação (RNF0035/RNF0021). O service faz `refresh` para ler o valor. |
| O que garante a auditoria completa (RNF0012)? | Escrita e registro de auditoria na **mesma transação**: `AuditoriaService` usa `Propagation.MANDATORY` — se a operação falhar, a auditoria não fica órfã; se a auditoria falhar, tudo dá rollback (comprovado por `ClienteIntegridadeTest`). |
| Como evitam dois cartões preferenciais? | Bloqueio pessimista da linha do cliente (`PESSIMISTIC_WRITE`) + índice único parcial no banco. Mesma estratégia usada nas trocas de endereço preferencial e no ativar/inativar de produto. |
| O cliente pode ficar sem endereço de cobrança ou entrega? | Não — RN0021/RN0022: o último endereço ativo de cada finalidade é protegido, na alteração e na inativação (`EnderecoService.protegerMinimos`). |
| O que acontece com o preferencial quando é inativado? | Outro endereço/cartão ativo assume a preferência automaticamente, mantendo o invariante de exatamente um preferencial. |
| Inativar produto é o mesmo que esconder? | Situação e visibilidade são campos separados: inativar força `visivel = false`; ativar **não** republica automaticamente — a republicação é manual. |
| Motivo de ativação/inativação de produto é validado como? | Categoria deve existir no domínio correspondente (`categoria_ativacao_produto` / `categoria_inativacao_produto`) e a justificativa é obrigatória. Categoria inválida → 400 `CATEGORIA_PRODUTO_INVALIDA`; repetir a operação → 409. Tentativa inválida não gera auditoria. |

## 5. Testes e evidências

| Pergunta provável | Resposta preparada |
|---|---|
| Como os requisitos são comprovados? | Por testes Selenium que operam o navegador de verdade (`ClienteCrudIT`, `ClientePerfilIT`, `ProdutosCrudIT`) + testes de apoio (unitários/integração). Os nomes dos testes carregam os IDs RF/RN/RNF. |
| Onde fica a rastreabilidade requisito → teste? | Na matriz gerada em `backend/target/evidencias/matriz.html`; relatórios em `target/reports/surefire.html` e `failsafe.html`. |
| Quantos testes existem? | Na última verificação registrada (09/09/2026): 63 cenários Selenium + 25 testes de apoio. Na hora, cite os números da execução que acabar de rodar — nunca o relatório antigo. |
| Por que o cadastro feito pelo Selenium não aparece na minha tela da porta 4200? | O teste sobe uma instância própria da aplicação em porta aleatória com banco PostgreSQL exclusivo (Testcontainers), e cada cenário reinicializa sua massa. |
| Como RNF0011 (consulta em até 1 segundo) foi medido? | Massa de 1.000 clientes, aquecimento e 90 medições (30 de cada uma das três consultas) gravadas em `performance.csv` — na execução de referência, média de ~159 ms. |
| E se um teste falhar durante a apresentação? | Mostre o erro e a captura de tela, identifique cenário e requisito. Não substitua a comprovação por operação manual nem apresente relatório antigo como se fosse atual. |

## 6. Perguntas “pegadinha”

| Pergunta provável | Resposta preparada |
|---|---|
| Qual versão do DRS foi usada? | O repositório tem `DRS_LES_2_2026.docx`; alguns enunciados citam `DRS_LES_1_2026`. O trabalho usou o arquivo disponível e declara a diferença em vez de afirmar comparação com a versão ausente. |
| As figuras do DVP mostram o que está pronto? | Não — as figuras 1–17 e 20–22 são o sistema completo planejado/proposto. O que existe no código está nas figuras 18, 19, 23, 24 e 25 (modelo atual e relacional atual). |
| O que ainda é protótipo com localStorage? | Catálogo da loja, sacola, checkout, cupons e pedidos do perfil. Integrados à API real estão: `/admin/clientes`, `/admin/produtos` e `/perfil/{dados,seguranca,enderecos,cartoes}`. |
| As transações do histórico são vendas reais? | Não — são massa demonstrativa persistida para atender RF0025 e alimentar o ranking (RN0027). No sistema completo serão substituídas por Pedido/Pagamento reais. |
| Sem login, como o perfil da loja sabe qual cliente é? | A sessão escolhe um cliente real cadastrado e guarda só o identificador no `localStorage`; todas as operações passam pelas mesmas validações e auditoria da API. |
| Por que não tem DELETE na API? | Porque o DRS pede inativação (RF0023/RF0012), e a inativação é lógica: o registro, histórico, endereços e vínculos permanecem no banco para rastreabilidade. |

## Dicas finais

- Antes de responder, identifique em qual das três visões a pergunta está (**implementado** / **protótipo** em localStorage / **planejado** no DVP) e diga isso explicitamente — é o ponto que mais demonstra domínio do projeto.
- Se não souber a resposta, diga "não está nesta entrega e está declarado como pendência conhecida" e aponte a documentação ([ESTADO-ATUAL.md](../ai/ESTADO-ATUAL.md)).
- Nunca afirme cobertura além do que os testes comprovam; os identificadores RF/RN/RNF nos nomes dos testes são a sua fonte de rastreabilidade.
