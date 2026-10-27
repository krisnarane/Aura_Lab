package br.com.auralab.produtos;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.auralab.produtos.domain.Produto;
import br.com.auralab.produtos.repository.ProdutoRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProdutoApiTest extends br.com.auralab.PostgresTest {
  @Autowired MockMvc mvc;
  @Autowired ProdutoRepository repo;
  private static final String MOTIVO =
      """
   {"categoria":"RETORNO_AO_MERCADO","justificativa":"Novo lote chegou ao centro de distribuição."}
   """;
  private static final String CADASTRO =
      """
   {"nome":"Máscara Capilar Argila","marca":"Aura Hair","descricao":"Máscara nutritiva para cabelo.","imagemUrl":"https://example.com/mascara.png","categorias":["HIDRATANTES"],"preco":49.90,"estoque":30,"visivel":true}
   """;

  @Test
  void RF0016_RN0017_RNF0012_ativacaoExigeMotivoEAudita() throws Exception {
    mvc.perform(
            patch("/api/v1/produtos/3/ativacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MOTIVO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.codigo").value("PRD-000003"))
        .andExpect(jsonPath("$.ativo").value(true))
        .andExpect(jsonPath("$.visivel").value(false))
        .andExpect(jsonPath("$.categoriaAtivacao").value("RETORNO_AO_MERCADO"));
    assertThat(repo.findById(3L)).get().extracting(Produto::isAtivo).isEqualTo(true);
    String depois =
        jdbc.queryForObject(
            "select dados_novos::text from auditoria_produto where operacao='ATIVAR_PRODUTO'",
            String.class);
    assertThat(depois).contains("RETORNO_AO_MERCADO", "Novo lote chegou");
  }

  @Test
  void RF0016_deveRecusarAtivarProdutoJaAtivo() throws Exception {
    mvc.perform(
            patch("/api/v1/produtos/1/ativacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MOTIVO))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("PRODUTO_JA_ATIVO"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auditoria_produto where operacao='ATIVAR_PRODUTO'",
                Integer.class))
        .isZero();
  }

  @Test
  void RN0017_justificativaECategoriaObrigatorias() throws Exception {
    mvc.perform(
            patch("/api/v1/produtos/3/ativacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoria\":\"RETORNO_AO_MERCADO\",\"justificativa\":\"  \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.justificativa").exists());
    mvc.perform(
            patch("/api/v1/produtos/3/ativacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"categoria\":\"INVENTADA\",\"justificativa\":\"Lote disponível.\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CATEGORIA_PRODUTO_INVALIDA"))
        .andExpect(jsonPath("$.campos.categoria").exists());
    assertThat(
            jdbc.queryForObject("select ativo from produto where id=3", Boolean.class))
        .isFalse();
  }

  @Test
  void RF0012_RN0015_inativacaoExigeMotivoEAudita() throws Exception {
    mvc.perform(
            patch("/api/v1/produtos/1/inativacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"categoria\":\"FORA_DE_MERCADO\",\"justificativa\":\"Linha saiu do catálogo.\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativo").value(false))
        .andExpect(jsonPath("$.visivel").value(false))
        .andExpect(jsonPath("$.categoriaInativacao").value("FORA_DE_MERCADO"));
    mvc.perform(
            patch("/api/v1/produtos/1/inativacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"categoria\":\"FORA_DE_MERCADO\",\"justificativa\":\"Denovo.\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("PRODUTO_JA_INATIVO"));
  }

  @Test
  void RNF0021_cadastroGeraCodigoUnico() throws Exception {
    mvc.perform(post("/api/v1/produtos").contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigo").value("PRD-000004"))
        .andExpect(jsonPath("$.ativo").value(true))
        .andExpect(jsonPath("$.visivel").value(true));
  }

  @Test
  void RF0015_consultaFiltraPorCamposDeIdentificacao() throws Exception {
    mvc.perform(get("/api/v1/produtos").param("nome", "vitamina"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].codigo").value("PRD-000001"))
        .andExpect(jsonPath("$[1]").doesNotExist());
    mvc.perform(get("/api/v1/produtos").param("ativo", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].codigo").value("PRD-000003"))
        .andExpect(jsonPath("$[1]").doesNotExist());
    mvc.perform(get("/api/v1/produtos").param("codigo", "PRD-000002").param("ativo", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void RF0011_RF0014_RN0012_RNF0012_cadastroEdicaoECategoriasMultiplas() throws Exception {
    mvc.perform(get("/api/v1/categorias-produto"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(8));
    mvc.perform(post("/api/v1/produtos").contentType(MediaType.APPLICATION_JSON)
            .content(CADASTRO.replace("[\"HIDRATANTES\"]", "[\"HIDRATANTES\",\"SKINCARE\"]")))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.categorias.length()").value(2));
    mvc.perform(patch("/api/v1/produtos/4").contentType(MediaType.APPLICATION_JSON)
            .content(CADASTRO.replace("Máscara Capilar Argila", "Máscara Capilar Nova")
                .replace("49.90", "0.00").replace("30", "0")
                .replace("[\"HIDRATANTES\"]", "[\"FACE\",\"SKINCARE\"]")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.codigo").value("PRD-000004"))
        .andExpect(jsonPath("$.nome").value("Máscara Capilar Nova"))
        .andExpect(jsonPath("$.preco").value(0)).andExpect(jsonPath("$.estoque").value(0));
    String antes = jdbc.queryForObject("select dados_anteriores::text from auditoria_produto where operacao='EDITAR_PRODUTO'", String.class);
    String depois = jdbc.queryForObject("select dados_novos::text from auditoria_produto where operacao='EDITAR_PRODUTO'", String.class);
    assertThat(antes).contains("Máscara Capilar Argila", "HIDRATANTES");
    assertThat(depois).contains("Máscara Capilar Nova", "FACE", "SKINCARE");
  }

  @Test
  void RF0014_entradaInvalidaNaoAudita() throws Exception {
    for (String corpo : java.util.List.of(
        CADASTRO.replace("https://example.com/mascara.png", "file:///mascara.png"),
        CADASTRO.replace("HIDRATANTES", "INEXISTENTE"),
        CADASTRO.replace("[\"HIDRATANTES\"]", "[\"FACE\",\"FACE\"]"))) {
      mvc.perform(patch("/api/v1/produtos/1").contentType(MediaType.APPLICATION_JSON).content(corpo))
          .andExpect(status().isBadRequest());
    }
    assertThat(jdbc.queryForObject("select count(*) from auditoria_produto", Integer.class)).isZero();
  }

  @Test
  void RF0015_filtrosCombinaveisMarcaCategoriaSituacao() throws Exception {
    mvc.perform(get("/api/v1/produtos").param("marca", "aura derma")
            .param("categoria", "FACE").param("ativo", "true").param("nome", "sérum"))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].codigo").value("PRD-000002"))
        .andExpect(jsonPath("$[1]").doesNotExist());
  }

  @Test
  void RF0014_estoqueNaoPodeSerMenorQueReservaVigente() throws Exception {
    Long carrinho = jdbc.queryForObject("insert into carrinho(cliente_id,criado_em,atualizado_em) values (1,current_timestamp,current_timestamp) returning id", Long.class);
    Long item = jdbc.queryForObject("insert into item_carrinho(carrinho_id,produto_id,quantidade) values (?,1,3) returning id", Long.class, carrinho);
    jdbc.update("insert into reserva_estoque(cliente_id,carrinho_id,produto_id,item_carrinho_id,quantidade,estado,iniciada_em,vence_em) values (1,?,1,?,3,'ATIVA',current_timestamp,current_timestamp + interval '20 minutes')", carrinho, item);
    String corpo = CADASTRO.replace("30", "2");
    mvc.perform(patch("/api/v1/produtos/1").contentType(MediaType.APPLICATION_JSON).content(corpo))
        .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("ESTOQUE_INSUFICIENTE"));
    assertThat(jdbc.queryForObject("select estoque from produto where id=1", Integer.class)).isEqualTo(25);
    assertThat(jdbc.queryForObject("select count(*) from auditoria_produto", Integer.class)).isZero();
  }

  @Test
  void RF0014_RF0016_edicaoNaoMudaSituacaoNemRepublicaInativo() throws Exception {
    mvc.perform(patch("/api/v1/produtos/3").contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.visivel").exists());
    mvc.perform(patch("/api/v1/produtos/3").contentType(MediaType.APPLICATION_JSON)
            .content(CADASTRO.replace("\"visivel\":true", "\"visivel\":false")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false))
        .andExpect(jsonPath("$.visivel").value(false))
        .andExpect(jsonPath("$.codigo").value("PRD-000003"));
  }

  @Test
  void RF0014_precoEditadoPreservaValorHistoricoDoPedido() throws Exception {
    Long pedido = jdbc.queryForObject("insert into pedido(cliente_id,status,subtotal,desconto,frete,total,entrega_tipo_residencia,entrega_tipo_logradouro,entrega_logradouro,entrega_numero,entrega_bairro,entrega_cep,entrega_cidade,entrega_estado,entrega_pais,criado_em) values (1,'EM_PROCESSAMENTO',89.90,0,0,89.90,'Casa','Rua','Teste','1','Centro','01310100','São Paulo','SP','Brasil',current_timestamp) returning id", Long.class);
    jdbc.update("insert into item_pedido(pedido_id,produto_id,produto_codigo,produto_nome,preco_unitario,quantidade,subtotal) values (?,1,'PRD-000001','Creme Facial Vitamina C',89.90,1,89.90)", pedido);
    mvc.perform(patch("/api/v1/produtos/1").contentType(MediaType.APPLICATION_JSON).content(CADASTRO))
        .andExpect(status().isOk()).andExpect(jsonPath("$.preco").value(49.90));
    assertThat(jdbc.queryForObject("select preco_unitario from item_pedido where pedido_id=?", java.math.BigDecimal.class, pedido))
        .isEqualByComparingTo("89.90");
  }

  @Test
  void produtoInexistenteRetorna404() throws Exception {
    mvc.perform(
            patch("/api/v1/produtos/999/ativacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MOTIVO))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("PRODUTO_NAO_ENCONTRADO"));
  }
}
