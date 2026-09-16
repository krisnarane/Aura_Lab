# Arquitetura e modelo implementados

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
| MVC + Service Layer | `ClienteController`, `ProdutoController`, `ClienteService`, `EnderecoService`, `CartaoService`, `TransacaoService`, `AuditoriaService`, `ProdutoService`, `AuditoriaProdutoService` |
| Repository | `ClienteRepository`, `BandeiraRepository`, `TransacaoRepository`, `AuditoriaRepository`, `ProdutoRepository`, `CategoriaAtivacaoProdutoRepository`, `CategoriaInativacaoProdutoRepository`, `AuditoriaProdutoRepository` |
| Strategy | `PoliticaRankingCliente`, `PoliticaRankingPorCompras` |
| Specification | `ClienteSpecifications` para filtros combináveis |
| Adapter | `frontend/js/clientesApi.js`, `frontend/js/produtosApi.js` |
| Facade | `frontend/js/clientesService.js`, `frontend/js/produtosService.js` |
| Page Object | `ClientesAdminPage`, `ProdutosAdminPage`, `FormularioComponente`, `EnderecoComponente`, `CartaoComponente`, `ConfirmacaoComponente` |

Controllers não acessam repositories diretamente. `ClienteMapper` somente converte entidades para DTOs. `RegistroCliente` monta snapshots permitidos para auditoria. Essas dependências são verificadas por teste de arquitetura.

## Responsabilidade dos services

| Classe | Responsabilidade |
| --- | --- |
| `ClienteService` | Cadastrar, consultar, alterar, inativar cliente e alterar senha; calcular ranking por Strategy |
| `EnderecoService` | Listar, adicionar, alterar e inativar endereços; manter mínimos e preferência de entrega |
| `CartaoService` | Listar bandeiras/cartões, adicionar, escolher preferencial e inativar cartão |
| `TransacaoService` | Consultar lista e detalhe das transações demonstrativas do cliente |
| `AuditoriaService` | Registrar eventos dentro da transação corrente e consultar o histórico |
| `ProdutoService` | Cadastrar/listar produtos (mínimos), inativar e ativar produto com categoria e justificativa (RF0012/RF0016); listar categorias de motivo |
| `AuditoriaProdutoService` | Registrar eventos de produto na transação corrente |
| `ValidadorCliente` | Normalizações e validações reutilizadas |

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
```

- `Cliente` contém dados pessoais, `senhaHash`, situação e timestamps. O ranking é derivado.
- `Endereco` pertence ao cliente e contém apelido, composição postal, finalidades, preferência e situação.
- `Cartao` pertence ao cliente, referencia `Bandeira` e guarda titular, últimos quatro dígitos, preferência e situação.
- `TransacaoCliente` é demonstrativa e temporária.
- `AuditoriaCliente` registra contexto do cliente, entidade, identificador, operação, instante, ator e snapshots JSON permitidos.
- `Produto` contém nome, marca, preço, estoque, situação (`ativo`), visibilidade (`visivel`) e o último motivo de ativação e de inativação (categoria + justificativa + instante). Histórico completo fica em `AuditoriaProduto`; inativar força `visivel = false` e ativar não republica automaticamente.

As figuras canônicas do estado implementado são:

- `docs/dvp-uml-2026/24-modelo-atual.puml`
- `docs/dvp-uml-2026/25-modelo-relacional-atual.puml`
- `docs/crud-admin/modelo-atual.puml`
- `docs/crud-admin/modelo-relacional-atual.puml`

O modelo completo planejado está em `docs/dvp-uml-2026/10-modelo-dominio.puml`. Ele inclui módulos futuros de produto, carrinho, pedido, pagamento, estoque, troca e cupom; a presença no diagrama não significa implementação atual.

## Persistência e contratos

Flyway V1–V4 cria clientes, endereços, bandeiras, cartões, auditoria e transações demonstrativas. V5 cria produtos, as duas tabelas de domínio de motivo (ativação/inativação) e `auditoria_produto`; V6 traz a massa demonstrativa de produtos. Migrações aplicadas são imutáveis; mudanças novas entram na próxima versão.

A API usa `/api/v1`, com endpoints de cliente, senha, endereços, cartões, transações e auditoria em `ClienteController`, e endpoints de produtos e motivos em `ProdutoController`. A associação de recursos filhos ao cliente da URL deve ser sempre validada.

| Recurso | Endpoints atuais |
| --- | --- |
| Cliente | `POST /clientes`, `GET /clientes`, `GET /clientes/{id}`, `PUT /clientes/{id}`, `PATCH /clientes/{id}/inativacao` |
| Senha | `PUT /clientes/{id}/senha` |
| Endereço | `GET/POST /clientes/{id}/enderecos`, `PUT /clientes/{id}/enderecos/{enderecoId}`, `PATCH /clientes/{id}/enderecos/{enderecoId}/inativacao` |
| Cartão | `GET/POST /clientes/{id}/cartoes`, `PUT /clientes/{id}/cartoes/{cartaoId}/preferencial`, `PATCH /clientes/{id}/cartoes/{cartaoId}/inativacao` |
| Consulta vinculada | `GET /clientes/{id}/transacoes`, `GET /clientes/{id}/transacoes/{transacaoId}`, `GET /clientes/{id}/auditoria` |
| Domínio | `GET /bandeiras` |
| Produto | `GET/POST /produtos`, `GET /produtos/{id}`, `PATCH /produtos/{id}/inativacao`, `PATCH /produtos/{id}/ativacao` (corpo `{categoria, justificativa}`), `GET /categorias-ativacao-produto`, `GET /categorias-inativacao-produto` |

Escrita e auditoria devem permanecer na mesma transação. Senha, hash, PAN e CVV não podem aparecer em logs, respostas ou snapshots. Mudanças de cartão preferencial usam bloqueio do cliente e índice único parcial para evitar múltiplos preferenciais ativos.
