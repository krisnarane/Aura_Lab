# Estado atual e limites do Aura Lab

**Atualizado em 01/11/2026.**

## Implementado e integrado

O módulo administrativo de clientes funciona de ponta a ponta em `/admin/clientes`:

`HTML/JavaScript → clientesService → clientesApi → ClienteController → Services → Repositories JPA → PostgreSQL`

Ele implementa cadastro, consulta, alteração e inativação de cliente, além de endereços, cartões, alteração isolada de senha, consulta de transações demonstrativas, ranking e auditoria. O Spring Boot também serve o frontend pela mesma origem, na porta 4200 por padrão.

O módulo administrativo de produtos funciona de ponta a ponta em `/admin/produtos` seguindo o mesmo fluxo (`produtosService → produtosApi → ProdutoController`), com cadastro completo dos campos do Aura Lab, edição e consulta (RF0011/RF0014/RF0015) e ciclo de situação: inativação (RF0012/RN0015) e ativação (RF0016/RN0017) sempre com categoria e justificativa registradas em auditoria. Catálogo e detalhe da loja exibem imagem, descrição e categorias do banco.

O perfil do cliente da loja (`/perfil/dados`, `/perfil/seguranca`, `/perfil/enderecos` e `/perfil/cartoes`) também está integrado à API via `perfil-api.js` → `clientesService` → `clientesApi`. Como não há autenticação, a sessão da loja escolhe um **cliente real cadastrado no PostgreSQL** (identificador mantido em `localStorage`) e todas as alterações dessas quatro telas persistem no banco com as mesmas validações e auditoria do painel administrativo.

O UC05 usa PostgreSQL nas telas existentes de catálogo, produto, sacola, checkout, confirmação, pedidos/cupons do perfil e administração de pedidos/trocas. Reservas persistidas bloqueiam as unidades escolhidas; o prazo configurável de 20 minutos renova na última inclusão, com aviso cinco minutos antes. RN0032 ajusta ou remove itens automaticamente e persiste notificações. A compra registra EM_PROCESSAMENTO no histórico e, na própria finalização, a operadora simulada consulta o resultado configurado por cartão salvo, retornando APROVADA ou REPROVADA. A baixa física, o consumo dos créditos e o troco acontecem somente na aprovação; auditoria e demais efeitos compartilham a transação. O recebimento de troca sempre reentra no estoque.

O administrador despacha APROVADA para EM_TRANSPORTE e confirma ENTREGUE. O cliente solicita troca parcial ou integral; o administrador autoriza e recebe, decidindo explicitamente a reentrada. Recebimento gera um único cupom. Troca parcial mantém a compra ENTREGUE; integral acompanha EM_TROCA, TROCA_AUTORIZADA e TROCADO. Pedidos anteriores à V9 são somente leitura como legados. Interpretações sobre expiração de pedido, recebimento após autorização e valor líquido do cupom aguardam validação do professor; consulte [REQUISITOS-VENDAS.md](REQUISITOS-VENDAS.md).

Tecnologias atuais: Java 21, Spring Boot 3.5.5, Maven Wrapper, Spring MVC, Bean Validation, JPA/Hibernate, PostgreSQL 17, Flyway, HTML/CSS/Bootstrap/JavaScript, Selenium WebDriver e JUnit 5.

A consultora RNF0044 em `/consultora` e `/perfil/preferencias` usa V15 no PostgreSQL para preferências, mensagens, votos e versões de modelo. O chat exige um endpoint Gemini ajustado ativo; sem ele, informa indisponibilidade. A preparação local cria clientes e compras fictícios, e o exportador gera JSONL cumulativo. A execução real de tuning e a prova de respostas do endpoint dependem da configuração Google Cloud. Ver [REQUISITOS-IA.md](REQUISITOS-IA.md).

O resumo de frete agora exige um endereço ativo de entrega do perfil ou a UF de um novo endereço para estimativa; não calcula a faixa de demais estados por ausência de endereço. Foram adicionados cenários Selenium rastreados a CT026, CT032, CT035, CT038, CT039, CT041, CT042 e CT053 nas classes existentes `ProdutosCrudIT` e `VendasLojaIT`. A relação e os comandos estão em `docs/crud-admin/CASOS-DE-TESTE.md`; resultados dependem da execução e dos relatórios atuais.

## Decisões de escopo vigentes

- Não há login ou autenticação. `ADMIN_DEMO` é um texto técnico atribuído pelo servidor à auditoria.
- Não existe e não é necessária uma entidade `UsuarioAcesso` nesta etapa.
- O hash BCrypt permanece em `Cliente`. A troca de senha é coordenada por `ClienteService`.
- `CartaoService` também lista as bandeiras cadastradas. Não há `SenhaService` nem `BandeiraService` separados.
- Cliente, endereço e cartão são inativados logicamente, sem exclusão física; o DRS não pede ativação de cliente. Produto tem inativação **e** ativação lógicas com motivo (RF0012/RF0016 + RN0015/RN0017), conforme o grupo de livros do DRS.
- UC05 integrado nas rotas existentes; sem cancelamento do cliente, recusa de troca, despacho de troca pelo cliente ou prazo de sete dias para reembolso.
- `TransacaoCliente` projeta eventos reais de pedido, pagamento, troca e cupom. Origem LEGADO permanece identificada; ranking conta uma compra aprovada por pedido, sem somar pagamento ou cupom como outra compra. Fórmula de ranking é decisão de projeto.
- Somente dados fictícios de cartão devem ser usados. PAN e CVV são transitórios e nunca são persistidos ou auditados.

## Três visões que não devem ser confundidas

| Visão | Local | Significado |
| --- | --- | --- |
| CRUD integrado atual | `backend/`, `/admin/clientes`, `/perfil/{dados,seguranca,enderecos,cartoes}`, figuras 18, 19, 23, 24 e 25 | Código executável e validado com PostgreSQL e Selenium |
| Protótipo amplo | Demais rotas e `frontend/js/store.js` | Demonstração de interface e regras locais; parte ainda usa mocks e `localStorage` |
| Sistema completo planejado | Figuras gerais 1–17 e 22 em `docs/dvp-uml-2026/` | Arquitetura alvo; não deve ser apresentada como totalmente implementada |

## Pendências conhecidas

- Modelar campos editoriais exclusivos dos livros do DRS, RN0051 (maior custo entre custos diferentes), RN0014 (autorização para margem), IA, autenticação e RF0058 em incrementos próprios. Entrada em estoque, cálculo por grupo e inativação automática já estão integrados; a análise RF0055 por categoria usa RN0072/RN0074 e a legenda RNF0045.
- Comunicar e validar ambiguidades documentais com o professor antes de afirmar aderência sem ressalvas.
- Conciliar registros legados manualmente quando houver evidência segura; não fabricar aprovações ou movimentações retroativas.
- Restringir `CascadeType.ALL` nas associações atuais quando isso puder ser feito com migração e testes de preservação de histórico.
- Confirmar a versão do DRS quando o enunciado citar `DRS_LES_1_2026`; o arquivo local é `DRS_LES_2_2026.docx`.
# Incremento RF0055/RNF0043/RNF0011

`/admin/dashboard` consulta o histórico mensal de pedidos APROVADA, EM_TRANSPORTE e ENTREGUE no PostgreSQL via `/api/v1/admin/analise-vendas`. O gráfico de linhas compara categorias atuais dos produtos, com rateio do total histórico do pedido e meses sem vendas preenchidos com zero. A seleção ADMIN_DEMO/CLIENTE_DEMO e o 403 demonstram a restrição funcional; o identificador enviado pelo navegador é forjável e não implementa autenticação. A consulta usa o índice da V14 e é medida com 1.000 pedidos em Testcontainers. Ver `REQUISITOS-VENDAS.md` e `OPERACAO-E-TESTES.md`.
