-- Massa da demonstração RF0055: meses, categorias e estados distintos.
-- O pedido de novembro em TROCADO conserva entregue_em para provar o histórico.
create temporary table massa_analise_vendas (
  criado_em timestamptz not null,
  status varchar(30) not null,
  produto_id bigint not null,
  total numeric(12,2) not null,
  entregue_em timestamptz
) on commit drop;

insert into massa_analise_vendas values
 ('2026-08-12 10:00:00-03', 'ENTREGUE', 1, 89.90, '2026-08-15 10:00:00-03'),
 ('2026-09-08 10:00:00-03', 'ENTREGUE', 2, 129.90, '2026-09-11 10:00:00-03'),
 ('2026-10-10 10:00:00-03', 'APROVADA', 1, 89.90, null),
 ('2026-10-18 10:00:00-03', 'ENTREGUE', 2, 129.90, '2026-10-21 10:00:00-03'),
 ('2026-11-03 10:00:00-03', 'TROCADO', 1, 89.90, '2026-11-06 10:00:00-03'),
 ('2026-11-05 10:00:00-03', 'REPROVADA', 2, 129.90, null);

insert into pedido (
  cliente_id, status, subtotal, desconto, frete, total,
  entrega_tipo_residencia, entrega_tipo_logradouro, entrega_logradouro,
  entrega_numero, entrega_bairro, entrega_cep, entrega_cidade, entrega_estado,
  entrega_pais, criado_em, entregue_em, legado, pagamento_status
)
select 1, m.status, m.total, 0, 0, m.total,
       'Casa', 'Rua', 'das Flores', '120', 'Centro', '01310100',
       'São Paulo', 'SP', 'Brasil', m.criado_em, m.entregue_em, false,
       case when m.status = 'REPROVADA' then 'REPROVADO' else 'APROVADO' end
  from massa_analise_vendas m;

insert into item_pedido (
  pedido_id, produto_id, produto_codigo, produto_nome, preco_unitario, quantidade, subtotal
)
select p.id, m.produto_id, 'PRD-' || lpad(m.produto_id::text, 6, '0'),
       pr.nome, m.total, 1, m.total
  from pedido p
  join massa_analise_vendas m on m.criado_em = p.criado_em
  join produto pr on pr.id = m.produto_id
 where p.criado_em >= '2026-08-01 00:00:00-03'
   and p.criado_em < '2026-12-01 00:00:00-03';
