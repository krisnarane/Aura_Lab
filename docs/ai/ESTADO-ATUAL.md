# Estado atual e limites do Aura Lab

**Atualizado em 14/09/2026.**

## Implementado e integrado

O módulo administrativo de clientes funciona de ponta a ponta em `/admin/clientes`:

`HTML/JavaScript → clientesService → clientesApi → ClienteController → Services → Repositories JPA → PostgreSQL`

Ele implementa cadastro, consulta, alteração e inativação de cliente, além de endereços, cartões, alteração isolada de senha, consulta de transações demonstrativas, ranking e auditoria. O Spring Boot também serve o frontend pela mesma origem, na porta 4200 por padrão.

O módulo administrativo de produtos funciona de ponta a ponta em `/admin/produtos` seguindo o mesmo fluxo (`produtosService → produtosApi → ProdutoController`), com cadastro e consulta mínimos e o ciclo de situação completo: inativação (RF0012/RN0015) e **ativação (RF0016/RN0017)** sempre com categoria e justificativa registradas em auditoria. Livro do DRS corresponde a produto da loja.

Tecnologias atuais: Java 21, Spring Boot 3.5.5, Maven Wrapper, Spring MVC, Bean Validation, JPA/Hibernate, PostgreSQL 17, Flyway, HTML/CSS/Bootstrap/JavaScript, Selenium WebDriver e JUnit 5.

## Decisões de escopo vigentes

- Não há login ou autenticação. `ADMIN_DEMO` é um texto técnico atribuído pelo servidor à auditoria.
- Não existe e não é necessária uma entidade `UsuarioAcesso` nesta etapa.
- O hash BCrypt permanece em `Cliente`. A troca de senha é coordenada por `ClienteService`.
- `CartaoService` também lista as bandeiras cadastradas. Não há `SenhaService` nem `BandeiraService` separados.
- Cliente, endereço e cartão são inativados logicamente, sem exclusão física; o DRS não pede ativação de cliente. Produto tem inativação **e** ativação lógicas com motivo (RF0012/RF0016 + RN0015/RN0017), conforme o grupo de livros do DRS.
- Cadastro público, perfil, checkout, vendas, pagamento, estoque e trocas completos ainda não estão integrados a esta API.
- `TransacaoCliente` é uma estrutura persistida de demonstração para RF0025 e RN0027. No sistema completo ela deve ser substituída ou migrada para `Pedido`, `Pagamento`, `Troca` e `Cupom` reais.
- Somente dados fictícios de cartão devem ser usados. PAN e CVV são transitórios e nunca são persistidos ou auditados.

## Três visões que não devem ser confundidas

| Visão | Local | Significado |
| --- | --- | --- |
| CRUD integrado atual | `backend/`, `/admin/clientes`, figuras 18, 19, 23, 24 e 25 | Código executável e validado com PostgreSQL e Selenium |
| Protótipo amplo | Demais rotas e `frontend/js/store.js` | Demonstração de interface e regras locais; parte ainda usa mocks e `localStorage` |
| Sistema completo planejado | Figuras gerais 1–17 e 20–22 em `docs/dvp-uml-2026/` | Arquitetura alvo; não deve ser apresentada como totalmente implementada |

## Pendências conhecidas

- Integrar cadastro público, perfil e checkout à API real quando essa etapa for solicitada.
- Substituir a projeção `TransacaoCliente` pelas entidades reais de venda sem duplicar valores no ranking.
- Implementar módulos completos de produto, carrinho, pedido, pagamento, estoque, troca, cupom, analytics e Consultora Aura conforme o DVP quando entrarem no escopo.
- Restringir `CascadeType.ALL` nas associações atuais quando isso puder ser feito com migração e testes de preservação de histórico.
- Confirmar a versão do DRS quando o enunciado citar `DRS_LES_1_2026`; o arquivo local é `DRS_LES_2_2026.docx`.
