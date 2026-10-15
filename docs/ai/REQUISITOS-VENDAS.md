# Gestão de vendas conforme o DRS

Fonte normativa: `docs/DRS_LES_2_2026.docx`. Atualização: 12/10/2026. Os textos do DRS são requisitos; as interpretações abaixo são decisões de implementação aprovadas pelo usuário. Comunicação e validação pelo professor permanecem pendentes.

## Estados e responsabilidades

Finalizar cria EM_PROCESSAMENTO. Validar todas as formas gera APROVADA ou REPROVADA. Administrador despacha APROVADA para EM_TRANSPORTE e confirma ENTREGUE. Cliente solicita troca de item/quantidade ou integral; solicitação nasce EM_TROCA, autorização gera TROCA_AUTORIZADA e recebimento gera TROCADO com cupom. Compra parcial permanece ENTREGUE; integral acompanha a troca.

## Decisões de projeto e ambiguidades

1. Operadora simulada determinística, sem cobrança real, login ou autenticação. ADMIN_DEMO e CLIENTE_DEMO são atores técnicos definidos pelo servidor.
2. RN0044 é interpretada como bloqueio das unidades escolhidas, preservando disponibilidade das restantes. Prazo padrão parametrizado PT20M contado da última inclusão bem-sucedida; alterar/remover não renova. Aviso cinco minutos antes. Job periódico e operações críticas conferem vencimento com Clock injetável.
3. Reserva transferida ao pedido mantém prazo original. Se expirar enquanto EM_PROCESSAMENTO, o pedido fica REPROVADA por RESERVA_EXPIRADA, libera estoque/créditos e rejeita aprovação tardia. O DRS não explicita esse estado na expiração do pedido.
4. Recebimento exige TROCA_AUTORIZADA. A redação de RF0044/RN0042 menciona EM TROCA no recebimento; a sequência autorização → recebimento é interpretação adotada e necessita confirmação.
5. RF0045 não define valor do crédito: adota-se valor líquido histórico da mercadoria, rateando desconto promocional e resíduos por item/unidade, sem frete. Cupons de troca são pagamento, não novo desconto. Recebimentos acumulados não excedem esse valor.
6. Frete permanece na PoliticaFrete atual. Valores regionais e fórmula de ranking não são regras monetárias definidas pelo DRS.
7. RNF0044 aparece duplicado no DRS; inconsistente e fora do escopo deste incremento. Fonte UC/DVP anterior inclui cancelamento, recusa e despacho de troca pelo cliente e processamento separado; a revisão remove essas operações do UC05 integrado.

Nenhuma comunicação ao professor foi enviada por esta implementação. O grupo deve apresentar os itens 2–6 e registrar a resposta antes de declarar aderência sem ressalvas.

## Persistência e legado

V1–V8 permanecem intactas. V9 identifica pedidos preexistentes como LEGADO e somente leitura. Não infere pagamento, recompõe cupom utilizado nem fabrica baixa de estoque. Cupom preexistente conserva situação/valor. Transações demonstrativas ficam identificadas com origem LEGADO; eventos reais possuem referência única à entidade de origem. Ranking soma apenas PEDIDO aprovado real e pedidos pagos do legado identificado, sem duplicar PAGAMENTO ou CUPOM. A serialização global usa lock transacional PostgreSQL; é uma decisão acadêmica que deve ser reavaliada se o volume de escrita crescer.

## Matriz de rastreabilidade do incremento

A evidência é o relatório da execução atual, em `backend/target/surefire-reports`, `failsafe-reports`, `screenshots` e `evidencias`. Presença de método não prova sucesso: confira a execução correspondente. Métodos preservados do incremento anterior complementam os CTs novos.

| Requisito | Fluxo e tela | Operação e verificações automatizadas |
| --- | --- | --- |
| RF0031 | Catálogo e sacola | CarrinhoService; VendaApiTest.RF0031_RF0032_RN0031_adicionaAlteraRemoveEValidaEstoque; VendasLojaIT carrinho |
| RF0032 | Quantidade e sacola | PUT item; testes acima e CT035 |
| RF0033 | Checkout | Finalização do carrinho; VendaApiTest compra existente; CT038/CT039 |
| RF0034 | Checkout e frete | VendaService.resumo; CT041/CT042; PoliticaFreteTest |
| RF0035 | Endereço no checkout | Existente/novo, incorporar ou não; VendaApiTest.RF0035_RN0023_novoEnderecoSemIncorporarPreservaComposicaoNoPedido e novo endereço/cartão |
| RF0036 | Pagamento no checkout | Cartão existente/novo e cupons; VendaApiTest RF0035_RF0036; Selenium compra |
| RF0037 | Cupons no checkout | RN0033–RN0036 em VendaApiTest; Selenium CT061; reserva e consumo na aprovação |
| RF0038 | Confirmação | POST pedido EM_PROCESSAMENTO; CT053 e compras API/Selenium |
| RF0039 | Pedidos administrativos | Despacho somente APROVADA; CicloVendaApiTest ciclo completo; Selenium CT055 |
| RF0040 | Detalhe administrativo | Entrega somente EM_TRANSPORTE pelo administrador; CT055 |
| RF0041 | Detalhe do perfil | Troca parcial e limite acumulado; CicloVendaApiTest troca parcial/quantidade/antes entrega; CT058 |
| RF0042 | Administração de trocas | Autorizar e refletir integral; CicloVendaApiTest troca integral; CT059 |
| RF0043 | Administração de trocas | Listagem de solicitações; CT058/CT059; ConsultaPerformanceTest |
| RF0044 | Recebimento administrativo | Receber após autorização, TROCADO; CicloVendaApiTest recebimento/antes autorização; CT058/CT059 |
| RF0045 | Trocas e cupons | Cupom único no recebimento; RegrasCicloVendaTest rateio; CT058/CT059 com uso em nova compra |
| RF0053 | Aprovação administrativa | Baixa física atômica e única; CicloVendaApiTest ciclo/recusa/expiração; CT055/CT056 |
| RF0054 | Recebimento administrativo | Reentrada explícita ou não; CicloVendaApiTest parcial/integral; CT058/CT059 |
| RF0025 | Transações do cliente | TransacaoService projeta eventos reais/legado; CicloVendaApiTest histórico |
| RN0023 | Endereço no checkout | Campos completos inclusive sem incorporação; VendaApiTest RN0023 parametrizado |
| RN0024 | Cartão no checkout | Composição sem persistir PAN/CVV; VendaApiTest RN0024_RN0025 parametrizado |
| RN0025 | Bandeiras no checkout | Bandeira registrada; mesmos testes de cartão |
| RN0027 | Ranking | Compra aprovada conta uma vez; CicloVendaApiTest histórico; PoliticaRankingPorComprasTest |
| RN0028 | Aprovação/recusa | Baixa só aprovada, recursos liberados na recusa; CicloVendaApiTest e CT055/CT056 |
| RN0031 | Catálogo e sacola | Limite de estoque/reservas; VendaApiTest RN0031; CT032; concorrência última unidade |
| RN0032 | Sacola e checkout | Ajuste, alerta e remoção automáticos; CicloVendaApiTest RN0032; CT039/CT060 |
| RN0033 | Checkout | Um promocional; VendaApiTest RN0033; Selenium promocional |
| RN0034 | Checkout | Diversos cartões, mínimo R$10 por parcela; VendaApiTest RN0034; Selenium dois cartões |
| RN0035 | Checkout | Cupom integral e um cartão abaixo do mínimo; VendaApiTest RN0035; Selenium cupom + R$5 |
| RN0036 | Checkout e aprovação | Recusa redundância; exemplos R$50 e trocos R$5/10/25; VendaApiTest RN0036; CT061 |
| RN0037 | Validação administrativa | Revalida crédito e retorno por parcela; CicloVendaApiTest todas parcelas; CT055/CT056 |
| RN0038 | Validação administrativa | APROVADA/REPROVADA; ciclo API e CT055/CT056 |
| RN0039 | Despacho | APROVADA → EM_TRANSPORTE; CT055; RegrasCicloVendaTest |
| RN0040 | Entrega | Administrador → ENTREGUE; CT055 |
| RN0041 | Perfil | Solicitação EM_TROCA e integral acompanha; CT058/CT059 |
| RN0042 | Recebimento | TROCADO; CT058/CT059; decisão de sequência sujeita à confirmação |
| RN0043 | Solicitação de troca no perfil | Somente itens ENTREGUE; CicloVendaApiTest.RF0041_RN0041_RN0043_recusaTrocaAntesDaEntrega e CT058/CT059 |
| RN0044 | Sacola e reservas | Bloqueio por unidades, última inclusão, aviso; CicloVendaApiTest concorrência/prazo; CT057 |
| RN0045 | Sacola expirada | Retirada e desbloqueio; CT057; API expiração e aprovação tardia |
| RN0046 | Perfil após autorização | Notificação persistida; CT058/CT059; API trocas |
| RNF0011 | Consultas de vendas | ConsultaPerformanceTest até 1s com aquecimento e amostra; CSV performance-vendas |
| RNF0012 | Todas as escritas | Auditoria transacional; CicloVendaApiTest rollback; auditoria do carrinho e efeitos únicos |
| RNF0042 | Sacola expirada | Produto, motivo e prazo removidos visíveis; CT057 |
| RNF0034 | Endereço no checkout e perfil | Adicionar/alterar endereço isoladamente sem editar demais dados do cliente; VendaApiTest novo endereço com/sem incorporação e ClientePerfilIT endereços |

## Revisão da entrega

Os resultados devem ser lidos nos relatórios atuais. Erros, testes não executados e decisões pendentes impedem declarar conclusão integral. Entradas gerais de estoque, precificação completa, analytics, IA e autenticação ficam em outros incrementos. Não há cancelamento pelo cliente, recusa de troca, reembolso de sete dias ou etapa adicional para gerar cupom.

Carrinhos anteriores sem reserva são conciliados pela última inclusão auditada, preservando seu vencimento. Sem prazo auditável ou com prazo vencido, a massa é preservada como item retirado e exige reinclusão, sem fabricar bloqueio ou baixa anteriores.

## Correções da demonstração — 12/10/2026

A sacola apresenta avisos associados à reserva vigente; notificações antigas de expiração continuam preservadas no histórico do cliente, sem aparecer como alertas da reserva atual. Avisos de vencimento deixam de aparecer após renovação, expiração ou transferência ao pedido. Itens efetivamente retirados continuam visíveis até a reinclusão.

A listagem `/perfil/pedidos` permite abrir o detalhe pelo código do pedido e solicitar troca quando o estado permite. A confirmação de checkout centraliza código e situação. O histórico do detalhe projeta eventos de `auditoria_cliente` da entidade PEDIDO, com situação, data e ator; não expõe todos os snapshots da auditoria. RF0040/RN0040 atribuem explicitamente a confirmação de entrega ao administrador.

Verificação dirigida das correções: 23 testes de `CicloVendaApiTest` e três cenários Selenium (CT055, CT057 e CT058/CT059) aprovados; testes do frontend aprovados. Relatórios preservados em `backend/target/evidencias/revisao-sacola-pedidos`. A verificação foi executada em cópia temporária para evitar conflito de compilação com o ambiente aberto; não equivale a nova execução integral de todas as suítes.
