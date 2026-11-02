# Arquitetura e modelo implementados

## Incremento RNF0044

`/perfil/preferencias` usa `clientesService.js`/`clientesApi.js`; `/consultora` usa `consultoraApi.js`. Ambos chegam a `IaController` → `IaService` → `IaRepository` (JDBC/PostgreSQL V15). `IaService` monta o contexto com compras aprovadas e candidatos visíveis, chama `GeradorIa`/`GeminiGerador` no servidor e grava pergunta, resposta, versão e auditoria apenas depois de uma resposta válida. `cliente_preferencia` liga cliente e categoria; `ia_conversa` liga cliente a mensagens; `ia_avaliacao` permite um voto atual por resposta; `ia_modelo` registra o job e o endpoint ativo. O script `tools/ia/pipeline.py` é a ferramenta manual de exportação, revisão e nova versão. Consulte [REQUISITOS-IA.md](REQUISITOS-IA.md) para o limite entre código pronto e execução remota.

## Fluxo e padrões

```text
Tela administrativa
  → clientesService.js (Facade)
  → clientesApi.js (Adapter HTTP)
  → ClienteController (MVC)
  → Service Layer
  → Spring Data Repository
  → PostgreSQL
```

| Padrão | Participantes atuais |
| --- | --- |
| MVC + Service Layer | `ClienteController`, `ProdutoController`, `VendaController`, `ClienteService`, `EnderecoService`, `CartaoService`, `TransacaoService`, `AuditoriaService`, `ProdutoService`, `AuditoriaProdutoService`, `CarrinhoService`, `VendaService` |
| Repository | `ClienteRepository`, `BandeiraRepository`, `TransacaoRepository`, `AuditoriaRepository`, `ProdutoRepository`, `ProdutoSpecifications`, `GrupoPrecificacaoRepository`, `EntradaEstoqueRepository`, `CategoriaAtivacaoProdutoRepository`, `CategoriaInativacaoProdutoRepository`, `AuditoriaProdutoRepository`, `CarrinhoRepository`, `PedidoRepository`, `CupomRepository` |
| Strategy | `PoliticaRankingCliente`, `PoliticaRankingPorCompras`, `PoliticaFrete`/`PoliticaFretePorRegiao` |
| Specification | `ClienteSpecifications` para filtros combináveis |
| Adapter | `frontend/js/clientesApi.js`, `frontend/js/produtosApi.js` |
| Facade | `frontend/js/clientesService.js`, `frontend/js/produtosService.js` |
| Page Object | `ClientesAdminPage`, `ProdutosAdminPage`, `LojaPage`, `FormularioComponente`, `EnderecoComponente`, `CartaoComponente`, `ConfirmacaoComponente` |

Controllers não acessam repositories diretamente. `ClienteMapper` somente converte entidades para DTOs. `RegistroCliente` monta snapshots permitidos para auditoria. Essas dependências são verificadas por teste de arquitetura.

## Responsabilidade dos services

| Classe | Responsabilidade |
| --- | --- |
| `ClienteService` | Cadastrar, consultar, alterar, inativar cliente e alterar senha; calcular ranking por Strategy |
| `EnderecoService` | Listar, adicionar, alterar e inativar endereços; manter mínimos e preferência de entrega |
| `CartaoService` | Listar bandeiras/cartões, adicionar, escolher preferencial e inativar cartão |
| `TransacaoService` | Consultar lista e detalhe das transações demonstrativas do cliente |
| `AuditoriaService` | Registrar eventos dentro da transação corrente e consultar o histórico |
| `ProdutoService` | Cadastrar, editar e filtrar produtos com categorias, grupo de precificação, descrição e imagem; validar reservas antes de reduzir estoque; inativar e ativar com motivo (RF0012/RF0016) |
| `EntradaEstoqueService` | RF0051/RF0052: serializar entrada com reservas, somar estoque, recalcular preço pelo grupo e auditar na mesma transação |
| `InativacaoAutomaticaProdutos` / `JobInativacaoProdutos` | RF0013/RN0016: inativar elegíveis com ator SISTEMA por job ou comando administrativo |
| `AuditoriaProdutoService` | Registrar eventos de produto na transação corrente |
| `ValidadorCliente` | Normalizações e validações reutilizadas |
| `CarrinhoService` | Adicionar, alterar quantidade, remover e visualizar itens do carrinho validando estoque (RF0031/RF0032/RN0031) |
| `VendaService` | Realizar/finalizar compra (RF0033/RF0038), endereço (RF0035), frete (RF0034), composição de pagamento e RN0033–RN0036; gera cupom de troco e reaproveita `EnderecoService`/`CartaoService` para incorporar endereço/cartão novos |
| `AnaliseVendasService` | RF0055/RN0072/RN0074: consulta PostgreSQL dos três status contabilizáveis, período de até 24 meses, rateio determinístico e agrupamento mensal |

Não há `SenhaService`, `BandeiraService`, `AuthService` ou domínio `UsuarioAcesso` no desenho atual.

## Entidades e relacionamentos atuais

```text
Cliente 1 ── 1..* Endereco
Cliente 1 ── 0..* Cartao
Cartao  0..* ── 1 Bandeira
Cliente 1 ── 0..* TransacaoCliente
Cliente 1 ── 0..* AuditoriaCliente
Produto 0..* ── 1 CategoriaInativacaoProduto (último motivo)
Produto 0..* ── 1 CategoriaAtivacaoProduto (último motivo)
Produto 1 ── 0..* AuditoriaProduto
Produto 0..* ── 1..* CategoriaProduto
```

- `Cliente` contém dados pessoais, `senhaHash`, situação e timestamps. O ranking é derivado.
- `Endereco` pertence ao cliente e contém apelido, composição postal, finalidades, preferência e situação.
- `Cartao` pertence ao cliente, referencia `Bandeira` e guarda titular, últimos quatro dígitos, preferência e situação.
- `TransacaoCliente` é projeção persistida dos eventos reais, com origem LEGADO identificada.
- `AuditoriaCliente` registra contexto do cliente, entidade, identificador, operação, instante, ator e snapshots JSON permitidos.
- `Produto` contém nome, marca, descrição, URL de imagem externa, preço, estoque, categorias, situação (`ativo`), visibilidade (`visivel`) e o último motivo de ativação e de inativação. `produto_categoria` resolve a relação muitos para muitos. Histórico completo fica em `AuditoriaProduto`; inativar força `visivel = false` e ativar não republica automaticamente.
- `Produto` referencia um `GrupoPrecificacao` obrigatório e guarda o último `valorCusto`; `EntradaEstoque` guarda cada lançamento com quantidade, custo, data e preço calculado. A V13 preenche `PADRAO` nos produtos anteriores. `ProdutoSpecifications` combina os filtros administrativos de RF0015.

As figuras canônicas do estado implementado são:

- `docs/dvp-uml-2026/24-modelo-atual.puml`
- `docs/dvp-uml-2026/25-modelo-relacional-atual.puml`
- `docs/crud-admin/modelo-atual.puml`
- `docs/crud-admin/modelo-relacional-atual.puml`

O modelo completo planejado está em `docs/dvp-uml-2026/10-modelo-dominio.puml`. Ele inclui módulos futuros de produto, carrinho, pedido, pagamento, estoque, troca e cupom; a presença no diagrama não significa implementação atual.

## Persistência e contratos

Flyway V1–V4 cria clientes, endereços, bandeiras, cartões, auditoria e transações demonstrativas. V5 cria produtos, as duas tabelas de domínio de motivo (ativação/inativação) e `auditoria_produto`; V6 traz a massa demonstrativa de produtos. V7 cria as tabelas de vendas (`carrinho`, `item_carrinho`, `cupom`, `pedido`, `item_pedido`, `pagamento_parte`, com triggers de código `PED-`/`CUP-`); V8 carrega a massa demonstrativa de vendas (cartão de Marina e cupons). Migrações aplicadas são imutáveis; mudanças novas entram na próxima versão.

V11 acrescenta `categoria_produto`, `produto_categoria`, `descricao` e `imagem_url`, sem modificar V1–V10. As oito categorias controladas e os dados dos três produtos demonstrativos são semeados pela migração. A API exige ao menos uma categoria e URL HTTP(S); a edição consulta reservas vigentes sob o advisory lock do fluxo de vendas. `item_pedido.preco_unitario` continua sendo o valor histórico gravado na compra.

V12 cria `pedido_analise_entregue_criado`, índice parcial em `(criado_em,id)` para pedidos com `status='ENTREGUE'`. `AnaliseVendasController` valida o perfil demonstrativo via cabeçalho `X-Demo-Perfil`; o serviço valida período/categorias e usa `JdbcTemplate` para carregar pedidos, itens e categorias em uma consulta, agregando em memória. A rota `/admin/dashboard` chama essa API e desenha SVG com tabela acessível. Não há autenticação real nessa demonstração.

V14 substitui o índice parcial da V12 por `pedido_analise_contabilizavel_criado`, para `APROVADA`, `EM_TRANSPORTE` e `ENTREGUE`. A V13 cria `grupo_precificacao` e `entrada_estoque`, com restrições de quantidade/custo/data. O job de inativação usa o mesmo bloqueio global de vendas e é desativado no perfil de teste.

O módulo de vendas reutiliza a auditoria de clientes (`AuditoriaService` → `auditoria_cliente`) para RNF0012 e a `RegraNegocioException` compartilhada. O pedido guarda um snapshot do endereço de entrega e das parcelas de pagamento (sem PAN/CVV — apenas bandeira e quatro últimos dígitos na descrição). Entidades de vendas: `Carrinho` 1─* `ItemCarrinho`; `Pedido` 1─* `ItemPedido` e 1─* `PagamentoParte`, com `EnderecoEntrega` embutido; `Cupom` (PROMOCIONAL/TROCA). As coleções de itens e partes de pagamento do pedido usam carregamento em lote para evitar N+1 nas consultas cobertas pelo RNF0011.

A API usa `/api/v1`, com endpoints de cliente, senha, endereços, cartões, transações e auditoria em `ClienteController`, e endpoints de produtos e motivos em `ProdutoController`. A associação de recursos filhos ao cliente da URL deve ser sempre validada.

No resumo do checkout (RF0034), `VendaService.resumo` exige endereço ativo com finalidade de entrega do cliente ou a UF de um novo endereço para estimativa. Ausência de endereço/UF retorna `ENDERECO_OBRIGATORIO`; endereço do perfil inativo ou sem finalidade de entrega retorna `ENDERECO_ENTREGA_INVALIDO`. A finalização continua exigindo endereço completo (RF0035/RN0023).

| Recurso | Endpoints atuais |
| --- | --- |
| Cliente | `POST /clientes`, `GET /clientes`, `GET /clientes/{id}`, `PUT /clientes/{id}`, `PATCH /clientes/{id}/inativacao` |
| Senha | `PUT /clientes/{id}/senha` |
| Endereço | `GET/POST /clientes/{id}/enderecos`, `PUT /clientes/{id}/enderecos/{enderecoId}`, `PATCH /clientes/{id}/enderecos/{enderecoId}/inativacao` |
| Cartão | `GET/POST /clientes/{id}/cartoes`, `PUT /clientes/{id}/cartoes/{cartaoId}/preferencial`, `PATCH /clientes/{id}/cartoes/{cartaoId}/inativacao` |
| Consulta vinculada | `GET /clientes/{id}/transacoes`, `GET /clientes/{id}/transacoes/{transacaoId}`, `GET /clientes/{id}/auditoria` |
| Domínio | `GET /bandeiras` |
| Produto | `GET/POST /produtos`, `GET/PATCH /produtos/{id}`, `PATCH /produtos/{id}/inativacao`, `PATCH /produtos/{id}/ativacao` (corpo `{categoria, justificativa}`), `GET /categorias-produto`, `GET /categorias-ativacao-produto`, `GET /categorias-inativacao-produto` |
| Estoque e precificação | `GET /grupos-precificacao`, `GET/POST /produtos/{id}/entradas-estoque`, `POST /produtos/inativacao-automatica` |
| Carrinho | `GET /clientes/{id}/carrinho`, `POST /clientes/{id}/carrinho/itens`, `PUT /clientes/{id}/carrinho/itens/{itemId}`, `DELETE /clientes/{id}/carrinho/itens/{itemId}`, `POST /clientes/{id}/carrinho/resumo` |
| Pedido/Cupom | `POST /clientes/{id}/pedidos` (finalizar), `GET /clientes/{id}/pedidos`, `GET /clientes/{id}/pedidos/{pedidoId}`, `GET /clientes/{id}/cupons` |

Escrita e auditoria devem permanecer na mesma transação. Senha, hash, PAN e CVV não podem aparecer em logs, respostas ou snapshots. Mudanças de cartão preferencial usam bloqueio do cliente e índice único parcial para evitar múltiplos preferenciais ativos.


## Ciclo integrado de vendas V9

`pedidos-api.js` reutiliza as rotas `/admin/pedidos`, `/admin/pedidos/:id`, `/admin/trocas` e `/perfil/pedidos/:id`. Facade `vendasService` e Adapter `vendasApi` usam `CicloVendaController`. `CicloPedidoService` coordena operadora simulada, estoque e aprovação; `TrocaService` coordena troca, crédito e reentrada; `ReservaEstoqueService` usa Clock injetável e `JobReservaVenda` para sincronizar/expirar reservas. Nenhum controller depende de repository.

V9 acrescenta reservas, itens retirados, notificações, trocas/itens de troca, movimentações e identificação de legados. Restrições únicas impedem duplicação dos efeitos. Operações críticas usam advisory lock transacional PostgreSQL de ordem única (serialização global nesta etapa acadêmica). Alterações diretas de estoque fora da aplicação não adquirem esse lock; são reconciliadas pela sincronização. O histórico carrega metadados e cupons em lote. Reservas consultadas em leitura ou job podem escrever notificações e ajustes, por isso essas consultas possuem transação de escrita. `ReservaEstoqueService.sincronizar` grava por JDBC em tabelas mapeadas por JPA (`carrinho`, `item_carrinho`, `pedido`, `cupom`, `transacao_cliente`), então faz flush antes de ler e limpa o contexto de persistência **somente quando alterou uma dessas tabelas**. Isso é parte do contrato documentado do método: as entidades da operação devem ser carregadas depois de sincronizar, nunca antes. `CicloPedidoService.validar` descobre o cliente por projeção escalar (`PedidoRepository.clienteIdDoPedido`) justamente para carregar o pedido uma única vez, já conciliado.

Novos contratos: `GET /admin/pedidos`, `GET /admin/pedidos/{id}`, `POST /admin/pedidos/{id}/despacho`, `/entrega`; `POST /clientes/{cliente}/pedidos/{pedido}/trocas`; `GET /clientes/{cliente}/trocas`, `/trocas/{id}`, `/notificacoes`; `GET /admin/trocas`; `POST /admin/trocas/{id}/autorizacao`, `/recebimento`. Checkout e solicitação aceitam Idempotency-Key. Erros mantêm `{codigo,mensagem,campos}`. Nenhum dado comercial é importado do localStorage.

Na V10, `CheckoutService` orquestra `VendaService.finalizar` e `CicloPedidoService.validar` na mesma transação. `GatewayPagamentoSimulado` consulta `resultado_operadora_cartao` por `cartao_id` (padrão APROVADO sem configuração). A rota administrativa `/pagamento/validar` foi removida. `POST /clientes/{id}/pedidos` devolve o estado final da validação. `POST /admin/trocas/{id}/recebimento` não recebe decisão de estoque: a reentrada é obrigatória. Os dados de cartões não incorporados não são persistidos e seguem o resultado padrão.
