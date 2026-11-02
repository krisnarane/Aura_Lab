# RNF0044 — Consultora Aura

## Exigido, implementado e dependência externa

| Ponto | RNF0044 e atividade | Estado neste incremento |
| --- | --- | --- |
| Recomendações personalizadas | Histórico de compras e preferências | `/consultora` usa preferências explícitas e apenas compras com pagamento `APROVADO`; candidatos vêm dos produtos ativos, visíveis e com estoque. |
| Chat generativo | Busca, dúvidas e conteúdo relevante | O backend chama o endpoint **ajustado** do Gemini 3.1 Flash-Lite ativado e valida os IDs sugeridos. Cada resposta e versão ficam no PostgreSQL. |
| Avaliação | Útil ou não útil por resposta | Voto alterável, motivo opcional no negativo, persistência e auditoria na mesma transação. |
| Treinamento contínuo | Ajuste com vendas e feedback, inclusive novos dados | `tools/ia/pipeline.py` exporta cumulativamente, revisa respostas negativas e inicia um novo tuning job manual ou ativa um job criado pelo Console. A ativação depende de job concluído e pergunta de verificação. |
| Demonstração | Dois clientes com histórias diferentes | `tools/ia/preparar_demo.py` cria dois clientes fictícios, preferências distintas e quatro compras aprovadas pelo checkout. A apresentação precisa produzir votos pelo chat e executar novo ajuste para comprovar a incorporação. |

O DRS chama os itens de **livros**. Por decisão explícita do grupo, este incremento demonstra os **cosméticos atuais** do Aura Lab como produtos equivalentes. Isso deve ser dito na apresentação; se a avaliação exigir livros literalmente, o catálogo e a massa precisam mudar. O arquivo local é `DRS_LES_2_2026.docx`; não foi feita comparação com uma versão 1 ausente.

## Decisões de produto e dados

O cliente escolhe de uma a cinco categorias já cadastradas em `/perfil/preferencias`. A escolha é simples de explicar, pode ser alterada pelo próprio cliente e não depende de inferir interesses a partir de dados sensíveis. O identificador do cliente continua sendo selecionado no perfil demonstrativo, pois esta etapa ainda não tem autenticação. As preferências, mensagens e avaliações ficam no PostgreSQL; o `localStorage` conserva somente a seleção de cliente da sessão demonstrativa.

O plano inicial escolhia Gemini 2.5 Flash-Lite, mas o primeiro job concluído pelo grupo usa **Gemini 3.1 Flash-Lite**. A versão `aura-v1` registra essa base real no PostgreSQL; o comando de novas versões usa `AURA_IA_BASE_MODEL`, configurado como `gemini-3.1-flash-lite` no carregador local. Ambos oferecem ajuste supervisionado na plataforma Google Cloud e endpoint utilizável pelo SDK. O processamento da IA ocorre na nuvem, com custo de tuning, armazenamento e chamadas, dependência de internet, faturamento e permissões Google Cloud. Confirmar preço e cotas na conta/região antes de rodar outro job. O contexto da pergunta inclui preferências e compras recentes para personalização individual; **isso não substitui o ajuste**. O ajuste usa exemplos supervisionados derivados das vendas e avaliações, e é refeito cumulativamente a partir do banco.

Para votos negativos, o motivo não vira resposta alvo automaticamente. Um integrante revisa a interação e registra uma correção com `revisar`. O exportador bloqueia votos negativos sem correção. Votos positivos usam a resposta avaliada. Dados pessoais, cartão, endereço e pagamento não entram no JSONL. O manifesto contém IDs de pedido/item ou resposta, contagens, período e SHA-256. A exportação elimina exemplos idênticos e separa treino/validação deterministicamente por produto ou interação de feedback, para que exemplos da mesma origem não apareçam nos dois conjuntos.

O texto gerado pela IA não é fonte para preço e disponibilidade. O backend só aceita IDs presentes entre os candidatos atuais; o frontend exibe preço e disponibilidade consultados do catálogo. Em falha do Google Cloud ou JSON inválido, a API responde 503 e não grava uma resposta de regras locais. A pergunta permanece no campo para nova tentativa.

## Rastreabilidade

| Elemento | Arquivos principais | Evidência |
| --- | --- | --- |
| RNF0044.1 — integração e personalização | `ia/service/IaService.java`, `GeminiGerador.java`, `IaRepository.java`, V15 | `IaApiTest` e verificação real separada do endpoint |
| RNF0044.2 — chat | `ia/api/IaController.java`, `frontend/js/consultora-aura.js` | `IaApiTest`, adaptador frontend e roteiro presencial |
| RNF0044.3 — avaliação e ajuste | V15, `tools/ia/pipeline.py` | `IaApiTest`, JSONL/manifesto, job e versão ativa |

O job `projects/871048686403/locations/us-central1/tuningJobs/8502433884703555584` concluiu e a pergunta de verificação ao endpoint `projects/871048686403/locations/us/endpoints/9096180531753648128` retornou JSON válido em 02/11/2026. A versão `aura-v1` está ativa no banco local de demonstração. Um teste com gerador simulado comprova o contrato e a persistência, enquanto a chamada de verificação comprova a resposta remota daquele endpoint; ainda é preciso verificar o fluxo completo pela tela.
