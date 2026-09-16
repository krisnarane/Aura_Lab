# Backend e testes do CRUD de clientes

Fluxo: `Tela → clientesService (Facade) → clientesApi (Adapter) → Controller → Service Layer → Repository → PostgreSQL`.

Padrões aplicados:

- MVC e Service Layer nos controllers e serviços separados de cliente, endereço, cartão, senha, transação e auditoria;
- Repository com Spring Data JPA;
- Strategy em `PoliticaRankingCliente`/`PoliticaRankingPorCompras`;
- Specification em `ClienteSpecifications` para filtros combináveis;
- Adapter e Facade nos módulos JavaScript `clientesApi.js` e `clientesService.js`;
- Page Object em `src/test/java/br/com/auralab/e2e/pages`.

O ArchUnit impede controllers de dependerem diretamente de repositories. As escritas e a auditoria executam na mesma transação. `ADMIN_DEMO` é um ator técnico fixo, pois esta entrega não possui login.

## Execução

Na raiz do projeto, inicie o PostgreSQL com `docker compose up -d`. Nesta pasta execute `./mvnw spring-boot:run` (Linux/macOS) ou `.\mvnw.cmd spring-boot:run` (Windows). A interface estará em `http://localhost:4200/admin/clientes`.

Execute todos os testes, incluindo Selenium em Chrome sem interface gráfica, com:

```powershell
.\mvnw.cmd verify "-Dselenium.headless=true"
```

Para apresentação em sala use `false`. Cada teste Selenium identifica no nome os RFs/RNs demonstrados. Capturas ficam em `target/screenshots`. Gere o relatório HTML com `.\mvnw.cmd surefire-report:report-only`.

Os testes usam PostgreSQL 17 via Testcontainers, com Docker obrigatório e migrações Flyway reais. Execute `./testar-crud.ps1 -Visivel` para apresentação e relatórios. A RN0028 do DRS pertence ao processamento de pagamentos/estoque e não é declarada como coberta pelo CRUD.
