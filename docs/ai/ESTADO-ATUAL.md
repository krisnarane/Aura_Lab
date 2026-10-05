# Estado atual e limites do Aura Lab

**Atualizado em 05/10/2026.**

## Implementado e integrado

O módulo administrativo de clientes funciona de ponta a ponta em `/admin/clientes`:

`HTML/JavaScript → clientesService → clientesApi → ClienteController → Services → Repositories JPA → PostgreSQL`

Ele implementa cadastro, consulta, alteração e inativação de cliente, além de endereços, cartões, alteração isolada de senha, consulta de transações demonstrativas, ranking e auditoria. O Spring Boot também serve o frontend pela mesma origem, na porta 4200 por padrão.

O módulo administrativo de produtos funciona de ponta a ponta em `/admin/produtos` seguindo o mesmo fluxo (`produtosService → produtosApi → ProdutoController`), com cadastro e consulta mínimos e o ciclo de situação completo: inativação (RF0012/RN0015) e **ativação (RF0016/RN0017)** sempre com categoria e justificativa registradas em auditoria. Livro do DRS corresponde a produto da loja.

O perfil do cliente da loja (`/perfil/dados`, `/perfil/seguranca`, `/perfil/enderecos` e `/perfil/cartoes`) também está integrado à API via `perfil-api.js` → `clientesService` → `clientesApi`. Como não há autenticação, a sessão da loja escolhe um **cliente real cadastrado no PostgreSQL** (identificador mantido em `localStorage`) e todas as alterações dessas quatro telas persistem no banco com as mesmas validações e auditoria do painel administrativo.

O módulo de **vendas** está integrado de ponta a ponta na jornada visual principal da loja: catálogo `/`, detalhe `/produto/:id`, sacola `/sacola`, checkout `/checkout/endereco`, confirmação `/checkout/confirmacao/:id` e cupons `/perfil/cupons`. O fluxo usa `vendas-api.js` → `vendasService` → `vendasApi` → `VendaController`/`CarrinhoService`/`VendaService`, preservando os componentes e o estilo do protótipo sem manter um segundo frontend paralelo. Cobre o carrinho de compra (RF0031/RF0032/RN0031), a realização e finalização da compra criando o pedido com status **EM_PROCESSAMENTO** (RF0033/RF0038), a seleção de endereço existente ou novo com incorporação ao perfil (RF0035/RN0023), o cálculo de frete (RF0034, política por região), e a composição de pagamento com cartões e cupons obedecendo RN0033 (um cupom promocional), RN0034 (mínimo de R$ 10,00 por cartão), RN0035 (cartão abaixo do mínimo apenas com cupons) e RN0036 (cupom de troco sem cupons desnecessários). A validação/aprovação do pagamento, a baixa de estoque, o bloqueio/expiração do carrinho e o processo de troca ficam **fora do escopo desta fase**. Pedidos antigos do perfil (`/perfil/pedidos`) ainda seguem no store demonstrativo.

Tecnologias atuais: Java 21, Spring Boot 3.5.5, Maven Wrapper, Spring MVC, Bean Validation, JPA/Hibernate, PostgreSQL 17, Flyway, HTML/CSS/Bootstrap/JavaScript, Selenium WebDriver e JUnit 5.

## Decisões de escopo vigentes

- Não há login ou autenticação. `ADMIN_DEMO` é um texto técnico atribuído pelo servidor à auditoria.
- Não existe e não é necessária uma entidade `UsuarioAcesso` nesta etapa.
- O hash BCrypt permanece em `Cliente`. A troca de senha é coordenada por `ClienteService`.
- `CartaoService` também lista as bandeiras cadastradas. Não há `SenhaService` nem `BandeiraService` separados.
- Cliente, endereço e cartão são inativados logicamente, sem exclusão física; o DRS não pede ativação de cliente. Produto tem inativação **e** ativação lógicas com motivo (RF0012/RF0016 + RN0015/RN0017), conforme o grupo de livros do DRS.
- Carrinho e finalização de compra (até o status EM_PROCESSAMENTO) estão integrados à API nas rotas principais da loja. Validação/aprovação de pagamento, baixa de estoque, bloqueio/expiração do carrinho e trocas ainda não. Cadastro público segue no store demonstrativo.
- `TransacaoCliente` é uma estrutura persistida de demonstração para RF0025 e RN0027. No sistema completo ela deve ser substituída ou migrada para `Pedido`, `Pagamento`, `Troca` e `Cupom` reais.
- Somente dados fictícios de cartão devem ser usados. PAN e CVV são transitórios e nunca são persistidos ou auditados.

## Três visões que não devem ser confundidas

| Visão | Local | Significado |
| --- | --- | --- |
| CRUD integrado atual | `backend/`, `/admin/clientes`, `/perfil/{dados,seguranca,enderecos,cartoes}`, figuras 18, 19, 23, 24 e 25 | Código executável e validado com PostgreSQL e Selenium |
| Protótipo amplo | Demais rotas e `frontend/js/store.js` | Demonstração de interface e regras locais; parte ainda usa mocks e `localStorage` |
| Sistema completo planejado | Figuras gerais 1–17 e 20–22 em `docs/dvp-uml-2026/` | Arquitetura alvo; não deve ser apresentada como totalmente implementada |

## Pendências conhecidas

- Integrar cadastro público e a tela antiga de pedidos do perfil à API real quando essa etapa for solicitada; catálogo, sacola, checkout, confirmação e cupons já usam a jornada integrada.
- Substituir a projeção `TransacaoCliente` pelas entidades reais de venda sem duplicar valores no ranking.
- Evoluir produto, carrinho, pedido, pagamento e cupom além do recorte atual e implementar estoque, troca, analytics e Consultora Aura conforme o DVP quando entrarem no escopo.
- Restringir `CascadeType.ALL` nas associações atuais quando isso puder ser feito com migração e testes de preservação de histórico.
- Confirmar a versão do DRS quando o enunciado citar `DRS_LES_1_2026`; o arquivo local é `DRS_LES_2_2026.docx`.
