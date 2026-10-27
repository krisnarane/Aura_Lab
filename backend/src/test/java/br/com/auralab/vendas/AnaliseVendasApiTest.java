package br.com.auralab.vendas;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.auralab.PostgresTest;
import br.com.auralab.TempoVendaTeste;
import br.com.auralab.vendas.api.dto.*;
import br.com.auralab.vendas.service.*;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TempoVendaTeste.class)
class AnaliseVendasApiTest extends PostgresTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired TempoVendaTeste.Relogio relogio;
  @Autowired VendaService vendas;
  @Autowired CicloPedidoService ciclo;

  @BeforeEach void tempo() { relogio.reiniciar(); }

  private void adicionar(long produto, int quantidade) throws Exception {
    mvc.perform(post("/api/v1/clientes/1/carrinho/itens")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(Map.of("produtoId", produto, "quantidade", quantidade))))
        .andExpect(status().isCreated());
  }

  private long comprar(long... produtos) throws Exception {
    for (long id : produtos) adicionar(id, 1);
    long cartao = jdbc.queryForObject("insert into cartao(cliente_id,bandeira_id,titular,ultimos_quatro,preferencial,ativo) "
        + "values(1,1,'MARINA COSTA','1111',false,true) returning id", Long.class);
    var resumo = mvc.perform(post("/api/v1/clientes/1/carrinho/resumo")
        .contentType(MediaType.APPLICATION_JSON).content("{\"enderecoId\":1}"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var total = json.readTree(resumo).path("total").decimalValue();
    long pedido = vendas.finalizar(1L, new CheckoutInput(1L, null, false, null, List.of(),
        List.of(new ParcelaCartaoInput(cartao, null, false, total)))).id();
    ciclo.validar(pedido);
    return pedido;
  }

  private void entregar(long pedido) throws Exception {
    mvc.perform(post("/api/v1/admin/pedidos/" + pedido + "/despacho")
        .contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk());
    mvc.perform(post("/api/v1/admin/pedidos/" + pedido + "/entrega")
        .contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isOk());
  }

  private JsonNode consultar(String inicio, String fim, String... categorias) throws Exception {
    var requisicao = get("/api/v1/admin/analise-vendas")
        .header("X-Demo-Perfil", "ADMIN_DEMO").param("inicio", inicio).param("fim", fim)
        .param("categorias", categorias);
    return json.readTree(mvc.perform(requisicao).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
  }

  @Test void RF0055_RNF0043_valorMensalEstadoAtualMesVazioEAtualizacaoAposEntrega() throws Exception {
    jdbc.update("delete from produto_categoria where produto_id=1");
    jdbc.update("insert into produto_categoria values(1,'FACE')");
    long outubro = comprar(1);
    var antes = consultar("2026-09-01", "2026-11-30", "FACE");
    assertThat(antes.path("meses").toString()).isEqualTo("[\"2026-09\",\"2026-10\",\"2026-11\"]");
    assertThat(antes.path("series").get(0).path("valores").get(1).decimalValue()).isEqualByComparingTo("0.00");
    entregar(outubro);
    var depois = consultar("2026-09-01", "2026-11-30", "FACE");
    BigDecimal total = jdbc.queryForObject("select total from pedido where id=?", BigDecimal.class, outubro);
    assertThat(depois.path("series").get(0).path("valores").get(1).decimalValue()).isEqualByComparingTo(total);
    assertThat(depois.path("series").get(0).path("valores").get(0).decimalValue()).isZero();
    relogio.avancar(Duration.ofDays(31));
    long novembro = comprar(1);
    entregar(novembro);
    assertThat(consultar("2026-10-01", "2026-10-31", "FACE").path("series").get(0).path("valores").size()).isEqualTo(1);
    assertThat(consultar("2026-11-01", "2026-11-30", "FACE").path("series").get(0).path("valores").get(0).decimalValue()).isPositive();
  }

  @Test void RF0055_rateioMultiplasCategoriasIncluiFreteECentavos() throws Exception {
    jdbc.update("delete from produto_categoria where produto_id in (1,2)");
    jdbc.update("insert into produto_categoria values(1,'FACE'),(1,'MAQUIAGEM'),(2,'SKINCARE')");
    long pedido = comprar(1, 2);
    entregar(pedido);
    var resultado = consultar("2026-10-12", "2026-10-12", "FACE", "MAQUIAGEM", "SKINCARE");
    var soma = BigDecimal.ZERO;
    for (var serie : resultado.path("series")) soma = soma.add(serie.path("valores").get(0).decimalValue());
    var total = jdbc.queryForObject("select total from pedido where id=?", BigDecimal.class, pedido);
    assertThat(soma).isEqualByComparingTo(total);
    assertThat(resultado.path("series").size()).isEqualTo(3);
    assertThat(total).isEqualByComparingTo("238.80");
    assertThat(resultado.path("series").get(0).path("valores").get(0).decimalValue()).isEqualByComparingTo("48.84");
    assertThat(resultado.path("series").get(1).path("valores").get(0).decimalValue()).isEqualByComparingTo("48.84");
    assertThat(resultado.path("series").get(2).path("valores").get(0).decimalValue()).isEqualByComparingTo("141.12");
    // A categoria atual reclassifica o histórico sem alterar o total do pedido.
    jdbc.update("delete from produto_categoria where produto_id=2");
    jdbc.update("insert into produto_categoria values(2,'OLHOS')");
    assertThat(consultar("2026-10-12", "2026-10-12", "SKINCARE")
        .path("series").get(0).path("valores").get(0).decimalValue()).isZero();
  }

  @Test void RF0055_datasCategoriasEPerfilInvalidos() throws Exception {
    mvc.perform(get("/api/v1/admin/analise-vendas").header("X-Demo-Perfil", "CLIENTE_DEMO")
        .param("inicio", "2026-10-01").param("fim", "2026-10-31").param("categorias", "FACE"))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    mvc.perform(get("/api/v1/admin/analise-vendas").param("inicio", "2026-10-01")
        .param("fim", "2026-10-31").param("categorias", "FACE"))
        .andExpect(status().isForbidden());
    for (String[] params : List.of(new String[]{"2026-11-01", "2026-10-01", "FACE"},
        new String[]{"2026-10-01", "2026-10-31", "INEXISTENTE"}))
      mvc.perform(get("/api/v1/admin/analise-vendas").header("X-Demo-Perfil", "ADMIN_DEMO")
          .param("inicio", params[0]).param("fim", params[1]).param("categorias", params[2]))
          .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/admin/analise-vendas").header("X-Demo-Perfil", "ADMIN_DEMO")
        .param("inicio", "2026-10-01").param("fim", "2026-10-31")
        .param("categorias", "FACE", "FACE")).andExpect(status().isBadRequest());
  }
}
