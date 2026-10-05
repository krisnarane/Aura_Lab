-- Massa demonstrativa de vendas para o aplicativo em execução.
-- Os cadastros de venda (cartão, cupons) usam funcionalidades já existentes ou são
-- pré-carregados, pois a geração de cupons de troca pelo processo de troca está fora do
-- escopo desta fase. Marina Costa é o cliente demonstrativo (CLI-000001).

-- Cartão preferencial de Marina (Visa). Número completo e CVV nunca são persistidos.
insert into cartao (cliente_id, bandeira_id, titular, ultimos_quatro, preferencial, ativo)
select c.id, b.id, 'MARINA COSTA', '1111', true, true
from cliente c, bandeira b
where c.codigo = 'CLI-000001' and b.nome = 'Visa';

-- Cupom promocional geral (10% de desconto).
insert into cupom (tipo, cliente_id, valor, percentual, situacao, validade, origem, criado_em)
values ('PROMOCIONAL', null, null, 10.00, 'ATIVO', null, 'Massa demonstrativa', current_timestamp);

-- Cupons de troca pré-carregados vinculados a Marina (créditos de compras anteriores).
insert into cupom (tipo, cliente_id, valor, percentual, situacao, validade, origem, criado_em)
select 'TROCA', c.id, 50.00, null, 'ATIVO', null, 'Crédito demonstrativo', current_timestamp
from cliente c where c.codigo = 'CLI-000001';
insert into cupom (tipo, cliente_id, valor, percentual, situacao, validade, origem, criado_em)
select 'TROCA', c.id, 30.00, null, 'ATIVO', null, 'Crédito demonstrativo', current_timestamp
from cliente c where c.codigo = 'CLI-000001';
