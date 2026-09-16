# Aura Lab

CRUD de clientes com interface JavaScript, API Spring Boot e PostgreSQL. O módulo cobre cadastro, consulta, alteração, inativação lógica, endereços, cartões, senha, transações, ranking e auditoria conforme o DRS do projeto.

## Executar

Pré-requisitos: Java 21, Docker Desktop e Chrome (Maven Wrapper incluído).

```powershell
docker compose up -d
cd backend
.\mvnw.cmd spring-boot:run
```

Acesse `http://localhost:4200/admin/clientes`.

O banco Docker atende em `127.0.0.1:5433` para evitar conflito com PostgreSQL do Windows na porta 5432. O backend já usa esse endereço por padrão.

## Testar

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd verify "-Dselenium.headless=true"
.\mvnw.cmd surefire-report:report-only
```

Para apresentar os testes com o navegador visível:

```powershell
.\mvnw.cmd verify "-Dselenium.headless=false"
```

O relatório fica em `backend/target/reports/surefire.html` e as capturas do Selenium em `backend/target/screenshots/`.

O Docker Compose e os testes usam PostgreSQL 17. Testcontainers cria um banco exclusivo, com massa reinicializada por cenário; Docker Desktop deve estar em execução. Não há fallback para H2. Execute `backend/testar-crud.ps1 -Visivel` para demonstrar e gerar relatórios e matriz de evidências.

Para agentes e novos colaboradores, comece pelo [contexto atual do projeto](docs/ai/README.md). Detalhes técnicos e rastreabilidade ficam na [documentação do CRUD](docs/crud-admin/README.md).
