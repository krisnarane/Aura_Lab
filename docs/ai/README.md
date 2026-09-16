# Contexto para agentes de IA — Aura Lab

Este diretório é o ponto inicial para trabalhar no Aura Lab. Ele evita misturar três visões diferentes do projeto: o CRUD administrativo já integrado, o protótipo amplo ainda baseado em `localStorage` e o sistema completo planejado no DVP.

## Leitura por tipo de tarefa

| Tarefa | Leia |
| --- | --- |
| Corrigir ou evoluir o CRUD de clientes | [ESTADO-ATUAL.md](ESTADO-ATUAL.md), [REQUISITOS-CLIENTES.md](REQUISITOS-CLIENTES.md) e [ARQUITETURA-ATUAL.md](ARQUITETURA-ATUAL.md) |
| Corrigir ou evoluir situação/cadastro de produtos | [ESTADO-ATUAL.md](ESTADO-ATUAL.md), [REQUISITOS-PRODUTOS.md](REQUISITOS-PRODUTOS.md) e [ARQUITETURA-ATUAL.md](ARQUITETURA-ATUAL.md) |
| Executar, testar ou preparar apresentação | [OPERACAO-E-TESTES.md](OPERACAO-E-TESTES.md) e [REQUISITOS-CLIENTES.md](REQUISITOS-CLIENTES.md) |
| Apresentar o CRUD em sala | [Roteiro de apresentação](../crud-admin/ROTEIRO-APRESENTACAO.md) — telas, falas, Selenium e código |
| Executar ou listar casos de teste | [Casos de teste](../crud-admin/CASOS-DE-TESTE.md) — requisito → cenário → comando por grupo |
| Alterar classes, relacionamentos ou banco | [ARQUITETURA-ATUAL.md](ARQUITETURA-ATUAL.md) e [../dvp-uml-2026/REGRAS-E-MIGRACAO.md](../dvp-uml-2026/REGRAS-E-MIGRACAO.md) |
| Alterar diagramas ou discutir o sistema completo | [ESTADO-ATUAL.md](ESTADO-ATUAL.md) e [../dvp-uml-2026/README.md](../dvp-uml-2026/README.md) |
| Trabalhar nas telas antigas do protótipo | [../fluxo-funcional-prototipo.md](../fluxo-funcional-prototipo.md) e [../matriz-rastreabilidade.md](../matriz-rastreabilidade.md), tratados como documentação histórica/parcial |

A skill local [aura-lab-project](../skills/aura-lab-project/SKILL.md) resume o fluxo de trabalho e as decisões que não devem ser redescobertas a cada tarefa. O arquivo [../../AGENTS.md](../../AGENTS.md) torna essa orientação visível na raiz do repositório.

## Fonte de verdade

- O DRS define o comportamento exigido.
- Código, migrações e testes definem o que está implementado agora.
- `docs/ai/` explica o estado atual e deve acompanhar mudanças relevantes.
- `docs/dvp-uml-2026/` contém arquitetura e modelos do sistema completo planejado, além de figuras específicas do estado atual.
- Documentos de agosto e o store do frontend descrevem o protótipo anterior; não provam persistência, transações ou integração com o backend.

Existe no repositório apenas `DRS_LES_2_2026.docx`, embora alguns enunciados recebidos mencionem `DRS_LES_1_2026`. Antes de afirmar aderência exata a outra versão, obtenha ou compare o arquivo correspondente.
