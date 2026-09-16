# CRUD de clientes — implementação e rastreabilidade

Documento complementar. Para orientar novos agentes, comece por [ai/README.md](ai/README.md), que separa o estado implementado, o protótipo histórico e o sistema completo planejado no DVP.

## Arquitetura

O módulo segue o fluxo `Tela → clientesService (Facade) → clientesApi (Adapter) → Controller → Service Layer → Repository → PostgreSQL`. DTOs isolam o contrato HTTP das entidades JPA e o `ApiExceptionHandler` padroniza erros em `400`, `404` e `409`.

| Padrão | Participantes | Extensão prevista |
| --- | --- | --- |
| MVC e Service Layer | `ClienteController`; services de cliente, endereço, cartão, transação e auditoria | Novo caso de uso entra em um service sem levar regra ao controller ou HTML. |
| Repository | repositories Spring Data JPA | Uma consulta nova pode usar métodos derivados ou Specification. |
| Strategy | `PoliticaRankingCliente` e `PoliticaRankingPorCompras` | Outra fórmula pode substituir a implementação sem alterar API ou tela. |
| Specification | `ClienteSpecifications` | Um filtro novo é combinado com os existentes usando `E`. |
| Adapter | `frontend/js/clientesApi.js` | Mudança no formato HTTP fica concentrada no adaptador. |
| Facade | `frontend/js/clientesService.js` | Telas consomem uma interface estável do módulo. |
| Page Object | `ClientesAdminPage`, `FormularioComponente`, `EnderecoComponente`, `CartaoComponente` e `ConfirmacaoComponente` | Mudança de seletor é corrigida nos Page Objects e componentes. |

Todas as dependências Java são injetadas por construtor. Controllers não acessam repositories diretamente, regra verificada por ArchUnit. Escrita e auditoria compartilham a mesma transação. O servidor usa o ator técnico `ADMIN_DEMO`; ele identifica o contexto demonstrativo e não uma pessoa autenticada.

## Regras implementadas

- RF0021/RN0021–RN0023/RN0026: cadastro completo com endereço inicial de cobrança e entrega.
- RF0022: alteração de dados pessoais preservando código e relacionamentos.
- RF0023: inativação lógica sem endpoint de exclusão ou reativação.
- RF0024: filtros isolados ou combinados por código, nome, CPF, e-mail, gênero, nascimento, telefone e situação.
- RF0025: consulta das transações persistidas do cliente.
- RF0026/RNF0034: múltiplos endereços e proteção do último endereço ativo de cobrança e entrega.
- RF0027/RN0024–RN0025: múltiplos cartões, bandeira de domínio e exatamente um preferencial quando existem cartões ativos.
- RF0028/RNF0031–RNF0033: alteração independente, confirmação, senha forte e BCrypt.
- RN0027: ranking de 1 a 5 pela soma das compras efetivadas. A fórmula é decisão do projeto, pois o DRS não a especifica.
- RN0028 pertence ao processamento de pagamento e estoque e não é declarada como coberta por este CRUD.

CPF, e-mail, telefone, CEP, data futura e duplicidades são validações complementares. Número completo e código de segurança do cartão são validados durante a requisição e descartados; persistem somente bandeira, titular e quatro últimos dígitos.

## Testes e apresentação

| Evidência automatizada | Requisitos principais |
| --- | --- |
| `ClienteApiTest` | RF0021, RF0023, RF0024, RN0021, RN0022, RN0026, RNF0031 e RNF0033 |
| `PoliticaRankingPorComprasTest` | RN0027 |
| `ArquiteturaTest` | separação Controller/Service/Repository |
| `ClienteCrudIT` por Selenium | RF0021–RF0028, RN0021–RN0027 e RNFs associados |

Os cenários Selenium usam Chrome, esperas explícitas e seletores `data-testid`. As operações avaliadas são feitas no navegador; o banco de testes é recriado pelas migrações Flyway. Use `mvn verify "-Dselenium.headless=false"` para a apresentação em sala. Os nomes dos métodos indicam diretamente os RFs e RNs demonstrados.

## Limites deliberados

O módulo administrativo não possui login. A massa de transações comprova consulta e ranking, sem implementar nesta entrega os processos de venda, pagamento ou estoque. As demais jornadas do protótipo continuam usando seu store demonstrativo; a administração de clientes não recorre a mocks quando a API falha.
