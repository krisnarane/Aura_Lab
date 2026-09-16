insert into produto(nome,marca,preco,estoque,ativo,visivel,criado_em,atualizado_em) values
  ('Creme Facial Vitamina C','Aura Derma',89.90,25,true,true,current_timestamp,current_timestamp),
  ('Sérum Ácido Hialurônico','Aura Derma',129.90,12,true,true,current_timestamp,current_timestamp);
insert into produto(nome,marca,preco,estoque,ativo,visivel,categoria_inativacao,justificativa_inativacao,inativado_em,criado_em,atualizado_em)
values ('Protetor Solar FPS 60','Solar Lab',59.90,0,false,false,'FORA_DE_MERCADO','Linha saiu de catálogo após esgotar o último lote.',current_timestamp,current_timestamp,current_timestamp);
