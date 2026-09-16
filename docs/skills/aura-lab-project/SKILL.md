---
name: aura-lab-project
description: Analyze, implement, test, or document work in the Aura Lab repository while reconciling its implemented customer CRUD, DRS requirements, full-system DVP, UML, and legacy frontend prototype. Use for requests about Aura Lab code, architecture, requirements, diagrams, PostgreSQL, or Selenium.
---

# Aura Lab Project

Start with [`../../ai/README.md`](../../ai/README.md), then load only the guide it routes to for the current task.

## Preserve the project boundaries

- Separate required behavior in the DRS, implemented behavior in code/tests, and future design in the DVP.
- Treat the integrated `/admin/clientes` module as current. Treat other frontend flows that still use `localStorage` as prototype behavior.
- Keep the solution without authentication unless the user changes that scope. `ADMIN_DEMO` is an audit label, not an authenticated user.
- Keep password change in `ClienteService` and brand lookup in `CartaoService`. Do not recreate `SenhaService`, `BandeiraService`, `UsuarioAcesso`, or generic service/repository layers without a new concrete need.
- Use logical inactivation for customer, address, and card. The available DRS names RF0023 as inactivation; the current API has no delete or reactivation.
- Keep PostgreSQL and Flyway as the persistence path. Never edit applied migrations; add a new version.
- Keep PAN, CVV, plaintext passwords, and password hashes out of responses, logs, and audit snapshots.

## Work from evidence

Inspect the relevant source before stating what exists. Read [`../../ai/REQUISITOS-CLIENTES.md`](../../ai/REQUISITOS-CLIENTES.md) for RF/RN/RNF mapping, [`../../ai/ARQUITETURA-ATUAL.md`](../../ai/ARQUITETURA-ATUAL.md) for classes and relationships, and [`../../ai/OPERACAO-E-TESTES.md`](../../ai/OPERACAO-E-TESTES.md) for commands and evidence.

When a change affects behavior, keep requirement identifiers in the corresponding test names and update the current-state documentation. Run the smallest meaningful verification, expanding to the full Selenium suite when the visible workflow or release readiness requires it.

## Handle DVP and UML correctly

Use `docs/dvp-uml-2026/*.puml` as diagram sources. Figures 1–17 describe the complete target architecture; figures 18, 19, 23, 24, and 25 document the current CRUD/test implementation; figures 20–22 are proposals. Check the package README before presenting a figure as implemented.

After editing PlantUML, run `docs/dvp-uml-2026/renderizar.ps1` and inspect its validation output. Keep diagrams simple enough for classroom presentation and consistent with the actual controller/service split.

## Note the DRS version

The repository currently contains `DRS_LES_2_2026.docx`. Some assignment text mentions `DRS_LES_1_2026`. Do not claim an exact comparison with the absent version; call out the filename mismatch when it matters.
