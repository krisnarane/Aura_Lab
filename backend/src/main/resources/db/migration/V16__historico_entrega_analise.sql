-- RF0055: a venda permanece contabilizável depois de atingir ENTREGUE,
-- inclusive quando uma troca integral muda o status corrente para TROCADO.
alter table pedido add column entregue_em timestamptz;

-- Dados atuais em ENTREGUE têm evidência direta. Pedidos em troca só são
-- recuperados quando a auditoria comprova a passagem por ENTREGUE.
update pedido p
set entregue_em = coalesce(
  (select min(a.ocorrida_em)
     from auditoria_cliente a
    where a.entidade = 'PEDIDO'
      and a.entidade_id = p.id
      and (a.dados_anteriores ->> 'status' = 'ENTREGUE'
        or a.dados_novos ->> 'status' = 'ENTREGUE')),
  p.criado_em)
where p.status = 'ENTREGUE'
   or exists (
      select 1
        from auditoria_cliente a
       where a.entidade = 'PEDIDO'
         and a.entidade_id = p.id
         and (a.dados_anteriores ->> 'status' = 'ENTREGUE'
           or a.dados_novos ->> 'status' = 'ENTREGUE'));

create index pedido_analise_entrega_criado
  on pedido (criado_em, id)
  where entregue_em is not null;

drop index if exists pedido_analise_contabilizavel_criado;
