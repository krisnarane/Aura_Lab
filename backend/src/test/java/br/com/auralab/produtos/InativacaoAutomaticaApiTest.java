package br.com.auralab.produtos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.auralab.PostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc
class InativacaoAutomaticaApiTest extends PostgresTest {
  @Autowired MockMvc mvc;

  @Test void RF0013_RN0016_inativaElegivelComAtorSistemaEIdempotencia() throws Exception {
    jdbc.update("update produto set estoque=0, preco=49.99 where id=1");
    mvc.perform(post("/api/v1/produtos/inativacao-automatica"))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value(1));
    assertThat(jdbc.queryForObject("select categoria_inativacao from produto where id=1", String.class))
        .isEqualTo("FORA_DE_MERCADO");
    assertThat(jdbc.queryForObject("select ator from auditoria_produto where produto_id=1", String.class))
        .isEqualTo("SISTEMA");
    mvc.perform(post("/api/v1/produtos/inativacao-automatica"))
        .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    assertThat(jdbc.queryForObject("select count(*) from auditoria_produto", Integer.class)).isEqualTo(1);
  }

  @Test void RF0013_naoInativaComEstoquePrecoAltoOuVendaAnterior() throws Exception {
    jdbc.update("update produto set estoque=0, preco=49.99 where id=1");
    jdbc.update("update produto set estoque=0, preco=50.00 where id=2");
    jdbc.update("insert into produto(nome,marca,descricao,imagem_url,preco,estoque,ativo,visivel,grupo_precificacao_codigo,criado_em,atualizado_em) values ('Estoque positivo','Aura','Teste','https://example.com/p.png',49.99,1,true,true,'PADRAO',current_timestamp,current_timestamp)");
    jdbc.update("insert into pedido(cliente_id,status,subtotal,desconto,frete,total,entrega_tipo_residencia,entrega_tipo_logradouro,entrega_logradouro,entrega_numero,entrega_bairro,entrega_cep,entrega_cidade,entrega_estado,entrega_pais,criado_em) values (1,'TROCADO',49.99,0,0,49.99,'Casa','Rua','Teste','1','Centro','01310100','São Paulo','SP','Brasil',current_timestamp)");
    jdbc.update("insert into item_pedido(pedido_id,produto_id,produto_codigo,produto_nome,preco_unitario,quantidade,subtotal) values (1,1,'PRD-000001','Creme',49.99,1,49.99)");
    mvc.perform(post("/api/v1/produtos/inativacao-automatica"))
        .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    assertThat(jdbc.queryForObject("select ativo from produto where id=1", Boolean.class)).isTrue();
  }
}
