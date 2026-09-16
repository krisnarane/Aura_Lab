insert into transacao_cliente(cliente_id,codigo,tipo,status,valor,ocorrida_em,detalhes)
select id,'PAG-DEMO-001','PAGAMENTO','APROVADO',459.90,'2026-09-01T12:00:00Z','Pagamento fictício do pedido PED-2026-001; cartão mascarado final 1111.' from cliente where codigo='CLI-000001';
insert into transacao_cliente(cliente_id,codigo,tipo,status,valor,ocorrida_em,detalhes)
select id,'TRO-DEMO-001','TROCA','TROCA_PROCESSADA',50,'2026-09-02T12:00:00Z','Troca fictícia de uma unidade do pedido PED-2026-001; gerou CUP-DEMO-001.' from cliente where codigo='CLI-000001';
insert into transacao_cliente(cliente_id,codigo,tipo,status,valor,ocorrida_em,detalhes)
select id,'CUP-DEMO-001','CUPOM','ATIVO',50,'2026-09-02T12:01:00Z','Crédito fictício de TRO-DEMO-001 vinculado ao cliente.' from cliente where codigo='CLI-000001';
