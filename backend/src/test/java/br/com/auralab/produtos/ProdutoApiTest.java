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
   {"nome":"Máscara Capilar Argila","marca":"Aura Hair","preco":49.90,"estoque":30,"visivel":true}
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
  void produtoInexistenteRetorna404() throws Exception {
    mvc.perform(
            patch("/api/v1/produtos/999/ativacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MOTIVO))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("PRODUTO_NAO_ENCONTRADO"));
  }
}
