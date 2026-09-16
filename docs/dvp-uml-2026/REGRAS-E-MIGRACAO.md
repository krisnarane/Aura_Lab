# Cardinalidades, restrições e migração

A arquitetura alvo é proposta; somente o esquema das figuras 24/25 existe na aplicação atual. Não executar SQL inferido dos diagramas diretamente no banco de demonstração.

## Clientes

- Endereço/cartão pertence obrigatoriamente a um cliente. Cartão referencia bandeira cadastrada.
- Cliente mantém ao menos um endereço ativo de cobrança e um de entrega; o mesmo pode cumprir ambos. `1..*` no domínio exige regra de serviço: FK não garante existência de filhos nem finalidades. A figura 25 mostra o limite físico da FK (`0..*`).
- Endereço preferencial precisa estar ativo e habilitado para entrega. Ao perder essa condição, escolher o elegível mais antigo. Não editar inativo.
- Com cartões ativos, exatamente um é preferencial. Hoje há lock do cliente e índice único parcial `(cliente_id) WHERE ativo AND preferencial`. O índice garante no máximo um; o service garante a existência. Índice semelhante para endereço é melhoria futura.
- CPF/e-mail únicos inclusive entre inativos; e-mail normalizado. Código gerado pelo banco é não nulo, único e imutável. Ranking é derivado.
- Ranking: `min(5, 1 + floor(total / 200))`, considerando somente pedidos em PAGAMENTO_REALIZADO, EM_TRANSITO ou ENTREGUE. Pagamento não é outra compra. Fórmula é decisão do projeto.
- Sem exclusão física ou reativação. Composição conceitual não implica `ON DELETE CASCADE`. O código ainda possui `CascadeType.ALL`; restringir às operações necessárias é melhoria pendente.
- Cartão persistido contém titular, bandeira, últimos quatro dígitos, preferência e situação. PAN/CVV são transitórios e também não entram em snapshots ou auditoria.

## Senha do cliente

Hash BCrypt permanece em Cliente. A troca de senha é operação do ClienteService, com validação e auditoria. Não há login, sessão ou classe de usuário. Bandeiras são consultadas pelo CartaoService.

## Auditoria

Hoje `auditoria_cliente.cliente_id` é obrigatório. Na proposta geral, o contexto de cliente é opcional. O ator é técnico, definido pelo servidor, sem conta autenticada.

`entidade` + `entidade_id` é referência descritiva, não FK polimórfica. O contexto de cliente possui FK própria. Escrita de negócio e auditoria compartilham transação; falha reverte a operação. Senha registra apenas alteração, sem conteúdo ou hash.

## Pedidos, pagamentos e créditos

- Cliente 1 → 0..* Pedido; Pedido 1 → 1..* ItemPedido; Pedido 1 → 1 PagamentoSimulado. Criar o conjunto transacionalmente.
- Pagamento 1 → 0..* PartePagamento. Valor de cada parte positivo; zero partes somente quando total final for zero. Soma das partes = total do pagamento/pedido.
- Parte CARTAO exige snapshot mascarado, aceita cartão salvo opcional e não possui cupom de troca. Parte CREDITO_TROCA exige cupom de troca, sem cartão/snapshot. Validar titularidade.
- Pedido aceita um cupom PROMOCIONAL; créditos TROCA entram nas partes, sem virarem outro desconto promocional.
- Cupom PROMOCIONAL exige modo de desconto e não tem titular/troca de origem. TROCA exige cliente e origem única, usando valor monetário.
- Crédito disponível = valor original menos partes RESERVADO ou CONSUMIDO. Reservar/consumir/liberar sob lock.
- Snapshots de cliente/endereço/produto preservam dados históricos. Cartão usa somente snapshot mascarado.
- Chave de idempotência única por cliente: mesmo conteúdo devolve pedido anterior; conteúdo diferente resulta em conflito. Expiração configurável; o DVP não fixa 20 minutos.
- Quantidades positivas, totais não negativos, validade ordenada e percentuais entre 0 e 100 exigem validação/constraints nas migrações futuras.

## Estoque e troca

- Produto 1 → 1 Estoque. Produto/Categoria N:N resolvido por PK composta em `produto_categoria`; mínimo de uma categoria por produto exige regra de conjunto.
- Reserva possui exatamente uma origem: item de carrinho OU item de pedido. CHECK `num_nonnulls(item_carrinho_id,item_pedido_id)=1`. Transferir origem no checkout e conferir produto do estoque.
- Disponível = físico menos reservas vigentes. Conferência e reserva usam lock/atualização atômica. Expiração libera reserva; aprovação converte em baixa física.
- BAIXA exige item de pedido; REENTRADA exige troca; ENTRADA não usa essas origens. Unicidades impedem baixa duplicada por item e reentrada duplicada por troca.
- Troca pertence a item e possui quantidade. Soma das quantidades em trocas não negadas não excede a comprada. Pedido permanece ENTREGUE.
- Troca pode ainda não ter cupom; quando TROCA_PROCESSADA exige exatamente um, ligado ao cliente do pedido. Reentrada é opcional conforme aptidão. Processamento, cupom, reentrada e auditoria são atômicos e idempotentes.
- Crédito proposto usa valor líquido histórico das unidades, desconto rateado e sem frete. Validar essa decisão na etapa de trocas.

## Migração preservando dados

1. Não alterar migrações já aplicadas; acrescentar versões.
2. Criar tabelas/domínios novos com CHECK, FK, índices e testes PostgreSQL.
3. Introduzir auditoria geral com compatibilidade temporária. Preservar timestamps, atores, texto legado e snapshots; não fabricar dados anteriores.
4. Implementar vendas por casos de uso; histórico consulta relacionamentos reais.
5. Manter `transacao_cliente` como fonte legada identificada até migrar/arquivar exemplos. Não deduzir itens reais de descrições fictícias nem contar compra duas vezes no ranking.
6. Integrar cadastro público/perfil/checkout à API, sem importar localStorage automaticamente.
7. Testar banco novo e atualização, rollback, concorrência e Selenium antes de retirar compatibilidade.

Analytics é serviço de consulta/exportação, sem necessidade de entidade artificial. BeautyAdvisor/GatewayPagamento são interfaces futuras, não tabelas. A figura 23 documenta testes atuais, não cobertura aprovada dos módulos futuros.
