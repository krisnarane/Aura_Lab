package br.com.auralab.produtos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.auralab.PostgresTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc
class EntradaEstoqueApiTest extends PostgresTest {
  @Autowired MockMvc mvc;
  private static final String ENTRADA = "{\"quantidade\":5,\"valorCusto\":\"33.33\",\"dataEntrada\":\"2026-10-12\"}";

  @Test void RF0051_RF0052_RNF0012_entradaSomaRecalculaEAudita() throws Exception {
    mvc.perform(post("/api/v1/produtos/1/entradas-estoque").contentType(MediaType.APPLICATION_JSON)
        .content(ENTRADA)).andExpect(status().isCreated())
        .andExpect(jsonPath("$.precoCalculado").value(50.00));
    mvc.perform(get("/api/v1/produtos/1")).andExpect(status().isOk())
        .andExpect(jsonPath("$.estoque").value(30)).andExpect(jsonPath("$.preco").value(50.00))
        .andExpect(jsonPath("$.valorCusto").value(33.33));
    mvc.perform(get("/api/v1/produtos/1/entradas-estoque")).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].quantidade").value(5));
    assertThat(jdbc.queryForObject("select operacao from auditoria_produto", String.class))
        .isEqualTo("ENTRADA_ESTOQUE");
  }

  @Test void RN0061_RN0062_RNF0064_entradasInvalidasNaoAlteramNemAuditam() throws Exception {
    for (String corpo : new String[] {
        ENTRADA.replace("\"quantidade\":5", "\"quantidade\":0"),
        ENTRADA.replace("\"quantidade\":5", "\"quantidade\":-1"),
        ENTRADA.replace("\"valorCusto\":\"33.33\"", "\"valorCusto\":0"),
        ENTRADA.replace("\"valorCusto\":\"33.33\"", "\"valorCusto\":-1"),
        ENTRADA.replace("\"valorCusto\":\"33.33\",", ""),
        ENTRADA.replace(",\"dataEntrada\":\"2026-10-12\"", "")}) {
      mvc.perform(post("/api/v1/produtos/1/entradas-estoque").contentType(MediaType.APPLICATION_JSON)
          .content(corpo)).andExpect(status().isBadRequest());
    }
    assertThat(jdbc.queryForObject("select estoque from produto where id=1", Integer.class)).isEqualTo(25);
    assertThat(jdbc.queryForObject("select count(*) from entrada_estoque", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from auditoria_produto", Integer.class)).isZero();
  }

  @Test void RF0051_produtoInexistente() throws Exception {
    mvc.perform(post("/api/v1/produtos/999/entradas-estoque").contentType(MediaType.APPLICATION_JSON)
        .content(ENTRADA)).andExpect(status().isNotFound());
  }
}
