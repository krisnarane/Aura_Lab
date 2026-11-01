-- RN0074: a análise de vendas passa a considerar APROVADA, EM_TRANSPORTE e ENTREGUE.
-- O índice parcial da V12 cobria apenas ENTREGUE e deixaria de atender a consulta (RNF0011).
drop index pedido_analise_entregue_criado;
create index pedido_analise_contabilizavel_criado
  on pedido (criado_em, id) where status in ('APROVADA', 'EM_TRANSPORTE', 'ENTREGUE');
