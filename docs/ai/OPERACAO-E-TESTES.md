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

O script executa `clean verify`; portanto, testes unitários/de integração rodam pelo Surefire e `ClienteCrudIT` roda pelo Failsafe. Não substitua PostgreSQL por H2 quando um teste falhar por falta de Docker.

## Última verificação conhecida

Em 09/09/2026, após a simplificação de services:

- 63 cenários Selenium aprovados;
- 25 testes de apoio aprovados;
- total de 88 testes, sem falhas, erros ou ignorados;
- PostgreSQL 17.11 e quatro migrações Flyway;
- 90 consultas com massa de 1.000 clientes: média de 158,77 ms e máximo de 415,60 ms;
- execução Selenium headless em Chrome 152.

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
