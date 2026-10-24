create table resultado_operadora_cartao (
  cartao_id bigint primary key references cartao(id),
  resultado varchar(12) not null check (resultado in ('APROVADO', 'REPROVADO', 'FALHA'))
);

insert into resultado_operadora_cartao (cartao_id, resultado)
select c.id, 'APROVADO' from cartao c
join cliente cl on cl.id = c.cliente_id
where cl.codigo = 'CLI-000001' and c.ultimos_quatro = '1111';

insert into cartao (cliente_id, bandeira_id, titular, ultimos_quatro, preferencial, ativo)
select cl.id, b.id, 'MARINA COSTA', '2222', false, true
from cliente cl, bandeira b
where cl.codigo = 'CLI-000001' and b.nome = 'Mastercard';

insert into resultado_operadora_cartao (cartao_id, resultado)
select c.id, 'REPROVADO' from cartao c
join cliente cl on cl.id = c.cliente_id
where cl.codigo = 'CLI-000001' and c.ultimos_quatro = '2222';
