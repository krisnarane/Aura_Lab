# Execução, testes e apresentação

## Pré-requisitos

- Java 21.
- Docker Desktop em execução.
- Chrome instalado para Selenium.
- Maven Wrapper incluído no repositório; não é necessário instalar Maven globalmente.
- Não é necessário instalar PostgreSQL localmente: Docker Compose fornece o banco da aplicação e Testcontainers fornece bancos descartáveis dos testes.

## Executar a aplicação

Na raiz do repositório:

```powershell
docker compose up -d
cd backend
.\mvnw.cmd spring-boot:run
```

Acesse `http://localhost:4200/admin/clientes`. As credenciais locais do banco de demonstração estão em `docker-compose.yml` e podem ser substituídas por `DB_URL`, `DB_USER` e `DB_PASSWORD`.

O PostgreSQL Docker do projeto usa `127.0.0.1:5433` no Windows e mantém a porta 5432 dentro do contêiner. Isso evita conflito com PostgreSQL instalado no Windows na porta 5432. O Spring usa essa porta 5433 por padrão. Se aparecer erro de senha, verifique primeiro o destino da conexão e eventuais variáveis `DB_URL`/`SPRING_DATASOURCE_URL`; não apague o volume nem redefina senhas antes de confirmar qual servidor respondeu.

## Executar os testes

Verificação completa em Chrome headless, com relatórios e matriz:

```powershell
cd backend
.\testar-crud.ps1
```

Apresentação com Chrome visível:

```powershell
cd backend
.\testar-crud.ps1 -Visivel
```

O script executa `clean verify`; portanto, testes unitários/de integração rodam pelo Surefire e os grupos `ClienteCrudIT`, `ClientePerfilIT`, `ProdutosCrudIT` e `VendasLojaIT` rodam pelo Failsafe. Não substitua PostgreSQL por H2 quando um teste falhar por falta de Docker.

Para validar somente a jornada do cliente escolhido no perfil:

```powershell
cd backend
.\mvnw.cmd clean test-compile failsafe:integration-test failsafe:verify "-Dit.test=ClientePerfilIT" "-Dselenium.headless=true"
```

`ClientePerfilIT` cobre a seleção de cliente ativo, edição dos dados pessoais, troca isolada de senha, manutenção de endereço e cadastro de cartão mascarado, além das rejeições de dados pessoais, senha, endereço e cartão inválidos. Os oito cenários conferem a resposta visual e a persistência/auditoria no PostgreSQL de Testcontainers.

Para a demonstração da Gestão de Vendas, os testes funcionais Selenium ficam em `VendasLojaIT` e percorrem a jornada visual principal: catálogo `/`, sacola `/sacola`, checkout `/checkout/endereco` e confirmação `/checkout/confirmacao/:id`. Eles cobrem o roteiro mínimo do enunciado: carrinho com mais de um item e alteração de quantidade (RF0031/RF0032/RN0031), compra com endereço e cartão já cadastrados, compra com novo endereço e cartão incorporados ao perfil (RF0035/RF0036), pagamento com dois cartões respeitando o mínimo (RN0034), cartão abaixo de R$ 10,00 combinado com cupom (RN0035), cupons que superam a compra gerando cupom de troco (RN0036) e pedido finalizado com status EM PROCESSAMENTO (RF0038/RN0033). As regras de pagamento também são cobertas em nível de API por `VendaApiTest` (Surefire).

Na apresentação do RF0034, explique o critério de frete adotado: base de R$ 15,00 para o Sudeste, R$ 20,00 para o Sul e R$ 30,00 para os demais estados, acrescida de R$ 2,00 por unidade; pedidos com subtotal a partir de R$ 300,00 têm frete grátis. Essa fórmula é uma decisão do projeto porque o DRS exige o cálculo, mas não define seus valores.

```powershell
cd backend
.\mvnw.cmd clean test-compile failsafe:integration-test failsafe:verify "-Dit.test=VendasLojaIT" "-Dselenium.headless=true"
```

## Última verificação conhecida

Em 05/10/2026, após a integração da Gestão de Vendas:

- 83 cenários Selenium aprovados: 63 de clientes, 8 de perfil, 4 de produtos e 8 de vendas;
- 56 testes de apoio aprovados;
- total de 139 testes, sem falhas, erros ou ignorados;
- PostgreSQL 17.11 e oito migrações Flyway;
- RNF0011 com massa de 1.000 clientes: máximo de 936,63 ms na listagem, 271,39 ms no filtro combinado e 104,16 ms na busca por código;
- RNF0011 nas consultas de vendas, após carregamento em lote das coleções do pedido: máximo de 79,33 ms no carrinho, 34,83 ms nos cupons, 259,85 ms na lista de pedidos e 47,34 ms no detalhe;
- execução Selenium headless em Chrome 154.

Esses números descrevem aquela execução. Após qualquer mudança, use os relatórios novos em vez de repetir os valores como prova atual.

## Evidências geradas

| Artefato | Local |
| --- | --- |
| Matriz requisito → teste → resultado | `backend/target/evidencias/matriz.html` |
| Medições do RNF0011 | `backend/target/evidencias/performance.csv` |
| Relatório de testes de apoio | `backend/target/reports/surefire.html` |
| Relatório Selenium | `backend/target/reports/failsafe.html` |
| Capturas em falhas | `backend/target/screenshots/` |

`backend/target` é gerado e não deve ser versionado. Testes de apresentação devem manter os identificadores RF/RN/RNF nos nomes. Testes puramente arquiteturais ou de infraestrutura podem ser classificados como apoio para não fingir vínculo com uma regra do DRS.

## Verificação proporcional à mudança

- Java de domínio/service: teste unitário ou de integração afetado e, se alterar comportamento visível, Selenium correspondente.
- Controller/DTO/erros: `ClienteApiTest` e Selenium do fluxo.
- Migração/JPA: `MigrationPersistenceTest` e testes PostgreSQL relevantes.
- Frontend administrativo: teste JavaScript aplicável e Selenium.
- Ranking: `PoliticaRankingPorComprasTest`, integridade e cenários de faixa no navegador.
- UML: execute `docs/dvp-uml-2026/renderizar.ps1` e verifique `validacao.json`; isso não requer Docker.

Não execute repetidamente a suíte completa depois que ela passou, a menos que outra alteração relevante tenha sido feita.
