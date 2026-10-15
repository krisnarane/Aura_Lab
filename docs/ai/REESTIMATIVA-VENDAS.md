# Reestimativa do incremento de vendas

Base: `G:\Meu Drive\Laboratório Engenharia de Software\Estimativa_Aura_Lab.xlsm`, aba Estimativas Aura Lab. Esta revisão acompanha o incremento integrado e deve ser aplicada ao acompanhamento do grupo. Estimativas em horas de esforço, sem afirmar horas reais gastas ou datas de entrega; papéis substituem nomes ainda não informados. E = (O + 4M + P)/6.

## Correções da organização original

| ID da fonte | Organização corrigida |
| --- | --- |
| VEN-01 a VEN-05, PED-01 | Código do carrinho, reservas, checkout e pagamento implementado; aceitação depende da regressão atual |
| PED-02 | Administrador despacha APROVADA → EM_TRANSPORTE → ENTREGUE; retirar EM_TRANSITO |
| EST-03 e EST-04 | Baixa somente após aprovação e liberação por reprovação/expiração; testes de atomicidade e idempotência |
| EST-01 e EST-02 | Entrada geral e precificação ficam fora deste incremento |
| POS-01 | Devolução, reembolso e sete dias ficam fora do UC05 do DRS; não entram na soma de esforço deste incremento |
| POS-02 | Recebimento de troca com decisão explícita de reentrada e geração imediata de cupom |
| POS-03 | Testar baixa, liberação, reentrada e crédito de troca; excluir devolução/reembolso e entrada geral |
| QA-01 | Selenium nas telas atuais, incluindo troca parcial/integral e uso do crédito em nova compra |
| QA-02 | PostgreSQL/Testcontainers configurado; tratar falhas de conexão Docker antes de executar |
| QA-03 | Evidências atuais e aceitação do grupo; nenhuma aceitação manual foi fabricada |
| DOC-01, DOC-03 a DOC-06 | Conferir DRS, revisão UC/DVP, diagramas de estados, matriz e resultados atuais |

## Esforço restante estimado após a implementação

| Atividade | Papel | O | M | P | E | Dependência |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| Aceitação manual e apresentação | Grupo e representante do cliente | 1 | 2 | 3 | 2,00 | Regressão funcional e ressalva de desempenho |
| Revisão da entrega pelo grupo | Líder do grupo | 0,5 | 1 | 2 | 1,08 | Documentos revisados e resultados atuais |
| Preparar e registrar validação das ambiguidades | Analista de requisitos | 0,5 | 1 | 2 | 1,08 | Resposta do professor, prazo externo indeterminado |
| Total estimado | Grupo | | | | 4,17 | Não corresponde a calendário nem tempo efetivamente gasto |

As estimativas são uma revisão prospectiva do restante; não substituem evidências de execução nem a aprovação acadêmica. A estimativa anterior de regressão (2/4/8; E=4,33) e revisão visual/documental (1/2/4; E=2,17) fica preservada aqui como histórico. Essas atividades foram realizadas; os dois testes de desempenho permanecem reprovados. Em 12/10/2026, o usuário autorizou adiar a otimização e manter os tempos atuais; essa melhoria não entra no total restante. A comunicação ao professor permanece pendente.
