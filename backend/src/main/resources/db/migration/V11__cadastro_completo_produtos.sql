create table categoria_produto (
  codigo varchar(40) primary key,
  descricao varchar(80) not null unique
);
insert into categoria_produto(codigo,descricao) values
 ('FACE','Face'),('MAQUIAGEM','Maquiagem'),('LABIOS','Lábios'),('OLHOS','Olhos'),
 ('SKINCARE','Skincare'),('HIDRATANTES','Hidratantes'),('BRUMA','Bruma'),('LIMPEZA_FACIAL','Limpeza Facial');
alter table produto add column descricao text;
alter table produto add column imagem_url text;
create table produto_categoria (
  produto_id bigint not null references produto(id),
  categoria_codigo varchar(40) not null references categoria_produto(codigo),
  primary key(produto_id,categoria_codigo)
);
create index produto_categoria_categoria on produto_categoria(categoria_codigo,produto_id);
update produto set descricao = case nome
 when 'Creme Facial Vitamina C' then 'Creme facial hidratante com vitamina C para o cuidado diário.'
 when 'Sérum Ácido Hialurônico' then 'Sérum facial hidratante com ácido hialurônico.'
 else 'Protetor solar facial FPS 60 para uso diário.' end,
 imagem_url = case nome
 when 'Creme Facial Vitamina C' then 'https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b'
 when 'Sérum Ácido Hialurônico' then 'https://images.unsplash.com/photo-1620916566398-39f1143ab7be'
 else 'https://images.unsplash.com/photo-1556229010-6c3f2c9ca5f8' end;
insert into produto_categoria(produto_id,categoria_codigo)
select id,'FACE' from produto where nome in ('Creme Facial Vitamina C','Sérum Ácido Hialurônico','Protetor Solar FPS 60');
insert into produto_categoria(produto_id,categoria_codigo)
select id,'SKINCARE' from produto where nome in ('Creme Facial Vitamina C','Sérum Ácido Hialurônico','Protetor Solar FPS 60');
insert into produto_categoria(produto_id,categoria_codigo)
select id,'HIDRATANTES' from produto where nome in ('Creme Facial Vitamina C','Sérum Ácido Hialurônico');
update produto set descricao = 'Produto de beleza.' where descricao is null;
update produto set imagem_url = 'https://example.com/produto.png' where imagem_url is null;
alter table produto alter column descricao set not null;
alter table produto alter column imagem_url set not null;
