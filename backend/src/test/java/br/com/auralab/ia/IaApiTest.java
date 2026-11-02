package br.com.auralab.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.auralab.PostgresTest;
import br.com.auralab.ia.service.GeradorIa;
import br.com.auralab.ia.service.IaIndisponivelException;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class IaApiTest extends PostgresTest {
  @Autowired MockMvc mvc;
  @MockitoBean GeradorIa gerador;

  private void modelo() {
    jdbc.update("insert into ia_modelo(versao,modelo_base,endpoint,job_nome,dataset_sha256,manifesto,ativo) "
        + "values ('demo-v1','gemini-2.5-flash-lite','projects/p/locations/us-central1/endpoints/1',"
        + "'jobs/1',repeat('a',64),'{}'::jsonb,true)");
  }

  @Test
  void RNF0044_preferenciasPersistemEValidamCategorias() throws Exception {
    mvc.perform(put("/api/v1/clientes/1/preferencias").contentType(MediaType.APPLICATION_JSON)
        .content("{\"categorias\":[\"FACE\",\"SKINCARE\"]}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("FACE"));
    assertThat(jdbc.queryForObject("select count(*) from cliente_preferencia where cliente_id=1", Integer.class)).isEqualTo(2);
    mvc.perform(put("/api/v1/clientes/1/preferencias").contentType(MediaType.APPLICATION_JSON)
        .content("{\"categorias\":[\"NAO_EXISTE\"]}"))
        .andExpect(status().isBadRequest());
    assertThat(jdbc.queryForObject("select count(*) from cliente_preferencia where cliente_id=1", Integer.class)).isEqualTo(2);
  }

  @Test
  void RNF0044_chatPersisteAvaliaEIsolaCliente() throws Exception {
    modelo();
    when(gerador.gerar(anyString(), anyString()))
        .thenReturn("{\"texto\":\"Conheça o Creme Facial Vitamina C.\",\"produtosIds\":[1,3,999]}");
    String json = mvc.perform(post("/api/v1/clientes/1/chat/conversas/mensagens")
        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"O que recomenda?\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.mensagem.modeloVersao").value("demo-v1"))
        .andExpect(jsonPath("$.produtos.length()").value(1))
        .andReturn().getResponse().getContentAsString();
    long conversa = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).path("conversaId").asLong();
    long resposta = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).path("mensagem").path("id").asLong();
    mvc.perform(put("/api/v1/clientes/1/chat/respostas/" + resposta + "/avaliacao")
        .contentType(MediaType.APPLICATION_JSON).content("{\"util\":false,\"motivo\":\"Não combinou\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.util").value(false));
    mvc.perform(put("/api/v1/clientes/1/chat/respostas/" + resposta + "/avaliacao")
        .contentType(MediaType.APPLICATION_JSON).content("{\"util\":true}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.util").value(true));
    mvc.perform(get("/api/v1/clientes/1/chat/conversas/" + conversa))
        .andExpect(status().isOk()).andExpect(jsonPath("$[1].util").value(true));
    jdbc.update("update cliente set ativo=true where id=2");
    mvc.perform(get("/api/v1/clientes/2/chat/conversas/" + conversa)).andExpect(status().isNotFound());
    mvc.perform(put("/api/v1/clientes/2/chat/respostas/" + resposta + "/avaliacao")
        .contentType(MediaType.APPLICATION_JSON).content("{\"util\":false}"))
        .andExpect(status().isNotFound());
    assertThat(jdbc.queryForObject("select count(*) from ia_avaliacao", Integer.class)).isEqualTo(1);
  }

  @Test
  void RNF0044_falhaExternaNaoRegistraRespostaFalsa() throws Exception {
    modelo();
    when(gerador.gerar(anyString(), anyString())).thenThrow(new IaIndisponivelException());
    mvc.perform(post("/api/v1/clientes/1/chat/conversas/mensagens")
        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Ajude-me\"}"))
        .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.codigo").value("IA_INDISPONIVEL"));
    assertThat(jdbc.queryForObject("select count(*) from ia_conversa", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from ia_mensagem", Integer.class)).isZero();
  }

  @Test
  void RNF0044_contextoUsaApenasComprasAprovadasEPreferenciasDoCliente() throws Exception {
    modelo();
    jdbc.update("update cliente set ativo=true where id=2");
    mvc.perform(put("/api/v1/clientes/1/preferencias").contentType(MediaType.APPLICATION_JSON)
        .content("{\"categorias\":[\"FACE\"]}")).andExpect(status().isOk());
    mvc.perform(put("/api/v1/clientes/2/preferencias").contentType(MediaType.APPLICATION_JSON)
        .content("{\"categorias\":[\"LABIOS\"]}")).andExpect(status().isOk());
    inserirPedido(1, "APROVADO", "Compra aprovada de Marina");
    inserirPedido(1, "REPROVADO", "Compra reprovada de Marina");
    inserirPedido(2, "APROVADO", "Compra aprovada de outro cliente");
    when(gerador.gerar(anyString(), anyString()))
        .thenReturn("{\"texto\":\"Veja o catálogo.\",\"produtosIds\":[]}");
    mvc.perform(post("/api/v1/clientes/1/chat/conversas/mensagens")
        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Sugira um item\"}"))
        .andExpect(status().isOk());
    ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
    verify(gerador).gerar(anyString(), prompt.capture());
    assertThat(prompt.getValue()).contains("FACE", "Compra aprovada de Marina")
        .doesNotContain("Compra reprovada", "Compra aprovada de outro cliente", "LABIOS");
  }

  private void inserirPedido(int cliente, String pagamento, String nome) {
    Long pedido = jdbc.queryForObject("insert into pedido(cliente_id,status,subtotal,total,"
        + "entrega_tipo_residencia,entrega_tipo_logradouro,entrega_logradouro,entrega_numero,"
        + "entrega_bairro,entrega_cep,entrega_cidade,entrega_estado,entrega_pais,criado_em,"
        + "legado,pagamento_status) values (?,'APROVADA',10,10,'Casa','Rua','Demo','1',"
        + "'Centro','01310100','São Paulo','SP','Brasil',current_timestamp,false,?) returning id",
        Long.class, cliente, pagamento);
    jdbc.update("insert into item_pedido(pedido_id,produto_id,produto_codigo,produto_nome,"
        + "preco_unitario,quantidade,subtotal) values (?,1,'PRD-000001',?,10,1,10)", pedido, nome);
  }
}
