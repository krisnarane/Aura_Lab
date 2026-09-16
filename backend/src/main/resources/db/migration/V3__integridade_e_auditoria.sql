alter table endereco add column complemento varchar(100);
update cliente set codigo = 'CLI-' || lpad(id::text, greatest(6,length(id::text)), '0') where codigo is null;
alter table cliente alter column codigo set not null;
create function atribuir_codigo_cliente() returns trigger language plpgsql as $$
begin
 if TG_OP = 'INSERT' then NEW.codigo := 'CLI-' || lpad(NEW.id::text, greatest(6,length(NEW.id::text)), '0');
 elsif NEW.codigo is distinct from OLD.codigo then raise exception 'Código do cliente é imutável'; end if;
 return NEW;
end $$;
create trigger codigo_cliente before insert or update of codigo on cliente for each row execute function atribuir_codigo_cliente();
create unique index cliente_email_normalizado on cliente(lower(trim(email)));
update cartao set preferencial=false where not ativo;
with escolhidos as (select distinct on (cliente_id) id from cartao where ativo order by cliente_id, preferencial desc, id)
update cartao set preferencial=(id in (select id from escolhidos)) where ativo;
create unique index cartao_preferencial_ativo on cartao(cliente_id) where ativo and preferencial;
alter table auditoria_cliente add column entidade varchar(80) not null default 'LEGADO';
alter table auditoria_cliente add column entidade_id bigint;
alter table auditoria_cliente add column dados_anteriores jsonb;
alter table auditoria_cliente add column dados_novos jsonb;
alter table auditoria_cliente alter column alteracoes type text;
create index auditoria_cliente_data on auditoria_cliente(cliente_id, ocorrida_em desc);
create index transacao_cliente_consulta on transacao_cliente(cliente_id, ocorrida_em desc);
create index endereco_cliente on endereco(cliente_id);
create index cartao_cliente on cartao(cliente_id);
update transacao_cliente set status='PAGAMENTO_REALIZADO' where tipo='PEDIDO' and status in ('APROVADA','APROVADO');
update transacao_cliente set status='EM_TRANSITO' where tipo='PEDIDO' and status='EM_TRANSPORTE';
