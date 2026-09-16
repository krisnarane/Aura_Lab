insert into cliente(codigo,nome,cpf,email,genero,nascimento,telefone_tipo,telefone_ddd,telefone_numero,senha_hash,ativo,criado_em,atualizado_em)
values ('CLI-000001','Marina Costa','12345678909','marina@auralab.dev','Feminino','1992-05-18','Celular','11','987654321','$2a$10$q7HnMrtMmZtObq3spswQZ.XB7/1r9hhQSoD/cf3F9WT8QiYSG6WFu',true,current_timestamp,current_timestamp),
       ('CLI-000002','Cliente Inativo','52998224725','inativo@auralab.dev','Outro','1988-10-10','Celular','21','999999999','$2a$10$q7HnMrtMmZtObq3spswQZ.XB7/1r9hhQSoD/cf3F9WT8QiYSG6WFu',false,current_timestamp,current_timestamp);
insert into endereco(cliente_id,apelido,tipo_residencia,tipo_logradouro,logradouro,numero,bairro,cep,cidade,estado,pais,observacoes,cobranca,entrega,preferencial_entrega,ativo)
values (1,'Casa','Casa','Rua','das Flores','120','Centro','01310100','São Paulo','SP','Brasil','Portaria 24h',true,true,true,true),
       (2,'Residencial','Apartamento','Avenida','Atlântica','500','Copacabana','22010000','Rio de Janeiro','RJ','Brasil',null,true,true,true,true);
insert into transacao_cliente(cliente_id,codigo,tipo,status,valor,ocorrida_em,detalhes)
values (1,'PED-2026-001','PEDIDO','ENTREGUE',459.90,current_timestamp,'Pedido demonstrativo entregue'),
       (1,'PED-2026-002','PEDIDO','APROVADA',189.90,current_timestamp,'Pedido demonstrativo aprovado');
