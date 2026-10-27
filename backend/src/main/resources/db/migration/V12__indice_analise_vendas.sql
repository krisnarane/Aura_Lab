-- RF0055/RNF0011: status atual e período da compra são os filtros da análise.
create index pedido_analise_entregue_criado
  on pedido (criado_em, id) where status = 'ENTREGUE';
