# Kanban — incremento de Gestão de Vendas

Situação em 12/10/2026. Cada linha representa um cartão para registrar no board. Concluído significa implementação ou verificação descrita realizada; não significa aceitação acadêmica integral. Não foram registrados responsáveis nominais, horas reais ou comunicação ao professor.

## Concluído

| ID | Título do cartão | Trabalho realizado |
| --- | --- | --- |
| VEN-01 | Conferir requisitos de vendas no DRS | Conciliar RF0031–RF0045, estoque, histórico, ranking e regras associadas com UC, DVP e implementação. |
| VEN-02 | Mapear divergências e decisões do incremento | Registrar expiração de pedido, recebimento após autorização, crédito líquido de troca e política de frete. |
| VEN-03 | Criar migração V9 do ciclo de vendas | Acrescentar reservas, trocas, notificações, movimentos e validações sem alterar V1–V8. |
| VEN-04 | Preservar pedidos e cupons legados | Identificar pedidos antigos para consulta e impedir efeitos financeiros ou de estoque fabricados. |
| VEN-05 | Conciliar carrinhos anteriores à reserva | Preservar vencimento auditável ou retirar itens para reinclusão, mantendo seus registros. |
| VEN-06 | Implementar disponibilidade por reservas | Descontar apenas unidades reservadas da disponibilidade, mantendo estoque físico separado. |
| VEN-07 | Integrar reserva às operações do carrinho | Atualizar reservas na inclusão, alteração e remoção na mesma transação. |
| VEN-08 | Implementar prazo configurável de reserva | Renovar na última inclusão bem-sucedida; edição e remoção não renovam. |
| VEN-09 | Processar aviso e expiração | Usar Clock injetável e job; avisar cinco minutos antes, liberar recursos e registrar itens expirados. |
| VEN-10 | Sincronizar mudanças de disponibilidade | Ajustar quantidades, remover itens indisponíveis e persistir notificações. |
| VEN-11 | Atualizar contratos e erros das APIs | Expor disponibilidade, vencimento, itens retirados, ações permitidas, trocas e notificações. |
| VEN-12 | Validar composição do checkout | Validar endereço, cartões, mínimo por cartão, cupons, titularidade e redundância. |
| VEN-13 | Finalizar compra em processamento | Transferir reservas ao pedido e reservar créditos sem consumo antecipado. |
| VEN-14 | Criar operadora de pagamento simulada | Produzir aprovação, recusa e falha determinísticas por parcela. |
| VEN-15 | Implementar aprovação atômica | Revalidar recursos, baixar estoque, consumir créditos, emitir troco e auditar na mesma transação. |
| VEN-16 | Implementar reprovação e aprovação tardia | Liberar reservas e créditos; impedir aprovação após vencimento e efeitos repetidos. |
| VEN-17 | Implementar despacho e entrega administrativa | Restringir transições APROVADA → EM_TRANSPORTE → ENTREGUE. |
| VEN-18 | Implementar solicitação de troca parcial e integral | Exigir entrega e validar itens, quantidades e comprometimento em outras solicitações. |
| VEN-19 | Implementar autorização de troca | Alterar situação e persistir notificação para o cliente. |
| VEN-20 | Calcular crédito histórico da troca | Ratear desconto em centavos por item/unidade, sem frete e sem crédito superior ao valor líquido. |
| VEN-21 | Implementar recebimento e reentrada | Exigir autorização e decisão de reentrada; gerar um cupom e concluir a troca atomicamente. |
| VEN-22 | Proteger concorrência e idempotência | Usar bloqueios, chaves e restrições para impedir dupla reserva, baixa, reentrada ou emissão. |
| VEN-23 | Integrar carrinho e checkout nas telas existentes | Mostrar disponibilidade, avisos e itens retirados; atualizar dados pela API. |
| VEN-24 | Integrar administração de pedidos | Conectar listagem, detalhe, validação, despacho e entrega ao backend. |
| VEN-25 | Integrar perfil e administração de trocas | Conectar solicitação, autorização, recebimento, notificações e cupons nas telas existentes. |
| VEN-26 | Atualizar cabeçalho com dados reais | Exibir cliente e quantidade da sacola pela API nas jornadas integradas. |
| VEN-27 | Integrar histórico e ranking às vendas reais | Identificar legado e evitar contar pedido, pagamento e cupom como compras duplicadas. |
| VEN-28 | Criar testes unitários das regras | Verificar estados, rateio, crédito e arredondamento. |
| VEN-29 | Expandir testes de integração PostgreSQL | Verificar concorrência, expiração, pagamentos, rollback, trocas e efeitos únicos. |
| VEN-30 | Verificar migração de banco novo e existente | Testar aplicação das migrações e preservação do legado. |
| VEN-31 | Expandir Selenium nas telas existentes | Cobrir compra, pagamento, entrega, estoque, expiração, trocas e reutilização de cupom. |
| VEN-32 | Atualizar CT039 e rastreabilidade dos casos | Conferir remoção automática e manter RF/RN/RNF nos testes e documentação. |
| VEN-33 | Corrigir sincronização dos testes Selenium | Ajustar espera de recarga e elementos substituídos no DOM. |
| VEN-34 | Executar regressão funcional | Relatórios atuais: 82 testes backend funcionais/de apoio e 99 cenários Selenium aprovados; frontend aprovado. |
| VEN-35 | Medir desempenho das consultas | Executar dois testes: ambos falharam no limite de um segundo; máximos 1.646,51 ms em clientes e 1.027,18 ms em vendas. |
| VEN-36 | Atualizar arquitetura, estado atual e operação | Documentar APIs, domínio, banco, testes e limitações. |
| VEN-37 | Revisar diagramas UML | Atualizar casos de uso, sequências, estados e modelos de dados; gerar SVGs. |
| VEN-38 | Elaborar revisão do UC e complemento do DVP | Criar documentos Word e conferir suas páginas renderizadas. |
| VEN-39 | Revisar estimativa e organização das tarefas | Preservar estimativas originais, corrigir descrições e acrescentar esforço futuro com PERT. |

## Pendências

| ID | Título do cartão | Situação |
| --- | --- | --- |
| PEN-01 | Otimizar consultas para RNF0011 | Adiado por decisão do usuário. Manter tempos atuais e resultados de falha, sem aumentar o limite do teste. |
| PEN-02 | Executar aceitação manual do grupo | Aguardando validação dos fluxos nas telas existentes. |
| PEN-03 | Revisar entrega e apresentação com o grupo | Aguardando revisão humana dos documentos e demonstração. |
| PEN-04 | Comunicar e validar ambiguidades com o professor | Comunicação pendente; não declarar aderência sem ressalvas antes da confirmação. |

Os relatórios representam execuções acumuladas por suíte, incluindo reexecuções dirigidas; não uma única execução integral simultânea. Os dois testes de desempenho continuam com falha registrada.
