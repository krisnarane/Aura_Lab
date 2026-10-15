package br.com.auralab.vendas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de API do módulo de vendas: validam as regras do DRS antes da demonstração Selenium.
 * Marina Costa (cliente 1) e os produtos demonstrativos (1 e 2) vêm da massa reiniciada em {@link
 * br.com.auralab.PostgresTest}. Os cupons usados são pré-carregados via JDBC, pois a geração de
 * cupons de troca está fora do escopo — exceto o cupom de troco do RN0036.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VendaApiTest extends br.com.auralab.PostgresTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  private static final TypeReference<Map<String, Object>> MAPA_JSON = new TypeReference<>() {};
  private static final MediaType JSON = MediaType.APPLICATION_JSON;
  private static final long CLIENTE = 1L;
  private static final long ENDERECO = 1L; // Marina, SP, entrega (massa V2)

  // ---- RF0031/RF0032/RN0031: carrinho ----

  @Test
  void RF0031_RF0032_RN0031_adicionaAlteraRemoveEValidaEstoque() throws Exception {
    adicionar(1L, 2);
    var carrinho = adicionar(2L, 1); // dois itens no carrinho
    assertThat((List<?>) carrinho.get("itens")).hasSize(2);
    assertThat(new BigDecimal(carrinho.get("subtotal").toString()))
        .isEqualByComparingTo("309.70"); // 89.90*2 + 129.90

    Long itemId = itemIdDoProduto(1L);
    // Altera a quantidade do item (RF0032)
    var alterado = alterarQuantidade(itemId, 3);
    assertThat(unidades(alterado)).isEqualTo(4);

    // RN0031: não pode passar do estoque (produto 1 tem 25)
    mvc.perform(
            put(base() + "/carrinho/itens/" + itemId)
                .contentType(JSON)
                .content("{\"quantidade\":26}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("ESTOQUE_INSUFICIENTE"));

    // Remove um item
    var apos = remover(itemIdDoProduto(2L));
    assertThat((List<?>) apos.get("itens")).hasSize(1);
  }

  @Test
  void RN0031_recusaAdicionarAcimaDoEstoque() throws Exception {
    mvc.perform(
            post(base() + "/carrinho/itens")
                .contentType(JSON)
                .content("{\"produtoId\":2,\"quantidade\":13}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("ESTOQUE_INSUFICIENTE"));
  }

  // ---- RF0033/RF0035/RF0036/RF0038: compra com endereço e cartão já cadastrados ----

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"estado\":\"   \"}"})
  void RF0034_CT041_resumoSemEnderecoNaoCalculaFrete(String payload) throws Exception {
    adicionar(1L, 1);
    var itensAntes = jdbc.queryForList("select * from item_carrinho order by id");
    int auditoriasAntes =
        jdbc.queryForObject("select count(*) from auditoria_cliente", Integer.class);
    mvc.perform(post(base() + "/carrinho/resumo").contentType(JSON).content(payload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("ENDERECO_OBRIGATORIO"));
    assertThat(jdbc.queryForList("select * from item_carrinho order by id")).isEqualTo(itensAntes);
    assertThat(jdbc.queryForObject("select count(*) from pedido", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from auditoria_cliente", Integer.class))
        .isEqualTo(auditoriasAntes);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ativo=false", "entrega=false"})
  void RF0034_resumoRecusaEnderecoInativoOuSemFinalidadeDeEntrega(String alteracao)
      throws Exception {
    adicionar(1L, 1);
    jdbc.execute("update endereco set " + alteracao + " where id=1");
    mvc.perform(post(base() + "/carrinho/resumo").contentType(JSON).content("{\"enderecoId\":1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("ENDERECO_ENTREGA_INVALIDO"));
  }

  @Test
  void RF0033_RF0038_compraComEnderecoECartaoExistentes() throws Exception {
    adicionar(1L, 1);
    long cartao = criarCartao();
    BigDecimal total = resumoTotal(ENDERECO, null);

    Map<String, Object> payload =
        Map.of(
            "enderecoId", ENDERECO, "cartoes", List.of(Map.of("cartaoId", cartao, "valor", total)));
    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("EM_PROCESSAMENTO"))
        .andExpect(jsonPath("$.codigo").value(org.hamcrest.Matchers.startsWith("PED-")));

    assertThat(jdbc.queryForObject("select status from pedido", String.class))
        .isEqualTo("EM_PROCESSAMENTO");
    assertThat(jdbc.queryForObject("select total from pedido", BigDecimal.class))
        .isEqualByComparingTo(total);
    // Carrinho esvaziado após a finalização
    assertThat(jdbc.queryForObject("select count(*) from item_carrinho", Integer.class)).isZero();
    // RNF0012: auditoria registrada
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auditoria_cliente where operacao='FINALIZAR_COMPRA'",
                Integer.class))
        .isEqualTo(1);
  }

  // ---- RF0035/RF0036: novo endereço e novo cartão incorporados ao perfil ----

  @Test
  void RF0035_RF0036_RN0023_RN0024_novoEnderecoENovoCartaoIncorporados() throws Exception {
    adicionar(1L, 1);
    Map<String, Object> endereco = new LinkedHashMap<>(novoEndereco());
    endereco.put("observacoes", "Entregar na portaria");
    BigDecimal total = resumoTotal(null, "SP", null);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("novoEndereco", endereco);
    payload.put("incorporarEndereco", true);
    payload.put(
        "cartoes",
        List.of(
            Map.of(
                "novoCartao",
                cartaoNovo("4111111111111111", "123", 1),
                "incorporarCartao",
                true,
                "valor",
                total)));

    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("EM_PROCESSAMENTO"))
        // RN0023: o snapshot do pedido preserva a composição, inclusive observações
        .andExpect(jsonPath("$.entrega.observacoes").value("Entregar na portaria"))
        .andExpect(jsonPath("$.entrega.estado").value("SP"));

    // Endereço e cartão foram incorporados ao perfil
    assertThat(
            jdbc.queryForObject(
                "select count(*) from endereco where cliente_id=1 and apelido='Trabalho'",
                Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from cartao where cliente_id=1", Integer.class))
        .isEqualTo(1);
    // Dados sensíveis do cartão não persistidos
    assertThat(
            jdbc.queryForObject(
                "select ultimos_quatro from cartao where cliente_id=1", String.class))
        .isEqualTo("1111");
    assertThat(jdbc.queryForObject("select entrega_observacoes from pedido", String.class))
        .isEqualTo("Entregar na portaria");
  }

  /** RN0023: o cartão novo pode não ser incorporado; o endereço do pedido segue completo. */
  @Test
  void RF0035_RN0023_novoEnderecoSemIncorporarPreservaComposicaoNoPedido() throws Exception {
    adicionar(1L, 1);
    Map<String, Object> endereco = new LinkedHashMap<>(novoEndereco());
    endereco.put("observacoes", "Sem incorporar ao perfil");
    BigDecimal total = resumoTotal(null, "SP", null);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("novoEndereco", endereco);
    payload.put("incorporarEndereco", false);
    payload.put(
        "cartoes",
        List.of(
            Map.of(
                "novoCartao",
                cartaoNovo("4111111111111111", "123", 1),
                "incorporarCartao",
                false,
                "valor",
                total)));

    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.entrega.enderecoId").doesNotExist())
        .andExpect(jsonPath("$.entrega.observacoes").value("Sem incorporar ao perfil"));

    // Nada foi acrescentado ao perfil, mas o pedido guarda o endereço e o cartão mascarado
    assertThat(
            jdbc.queryForObject(
                "select count(*) from endereco where cliente_id=1 and apelido='Trabalho'",
                Integer.class))
        .isZero();
    assertThat(jdbc.queryForObject("select count(*) from cartao where cliente_id=1", Integer.class))
        .isZero();
    assertThat(jdbc.queryForObject("select entrega_bairro from pedido", String.class))
        .isEqualTo("Bela Vista");
    assertThat(jdbc.queryForObject("select descricao from pagamento_parte", String.class))
        .isEqualTo("Visa final 1111");
  }

  // ---- RN0034: múltiplos cartões, mínimo de R$ 10,00 por cartão ----

  @Test
  void RN0034_pagamentoComDoisCartoesRespeitandoMinimo() throws Exception {
    adicionar(1L, 1);
    long c1 = criarCartao();
    long c2 = criarCartao();
    BigDecimal total = resumoTotal(ENDERECO, null);
    BigDecimal parte2 = new BigDecimal("10.00");
    BigDecimal parte1 = total.subtract(parte2);
    Map<String, Object> payload =
        Map.of(
            "enderecoId",
            ENDERECO,
            "cartoes",
            List.of(
                Map.of("cartaoId", c1, "valor", parte1), Map.of("cartaoId", c2, "valor", parte2)));
    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("EM_PROCESSAMENTO"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from pagamento_parte where tipo='CARTAO'", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void RN0034_recusaCartaoAbaixoDoMinimoSemCupons() throws Exception {
    adicionar(1L, 1);
    long c1 = criarCartao();
    long c2 = criarCartao();
    BigDecimal total = resumoTotal(ENDERECO, null);
    BigDecimal parte2 = new BigDecimal("5.00");
    BigDecimal parte1 = total.subtract(parte2);
    Map<String, Object> payload =
        Map.of(
            "enderecoId",
            ENDERECO,
            "cartoes",
            List.of(
                Map.of("cartaoId", c1, "valor", parte1), Map.of("cartaoId", c2, "valor", parte2)));
    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CARTAO_MINIMO"));
  }

  // ---- RN0035: cupom + cartão, valor abaixo de R$ 10,00 no cartão ----

  @Test
  void RN0035_aceitaUmCartaoAbaixoDoMinimoComCupons() throws Exception {
    adicionar(1L, 1);
    long cartao = criarCartao();
    BigDecimal total = resumoTotal(ENDERECO, null);
    BigDecimal noCartao = new BigDecimal("5.00");
    BigDecimal noCupom = total.subtract(noCartao);
    long cupom = inserirCupomTroca(noCupom);
    Map<String, Object> payload =
        Map.of(
            "enderecoId", ENDERECO,
            "cuponsTroca", List.of(cupom),
            "cartoes", List.of(Map.of("cartaoId", cartao, "valor", noCartao)));
    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("EM_PROCESSAMENTO"));
    assertThat(jdbc.queryForObject("select situacao from cupom where id=?", String.class, cupom))
        .isEqualTo("RESERVADO");
    aprovarUltimoPedido();
    // A aprovação consome o crédito reservado
    assertThat(jdbc.queryForObject("select situacao from cupom where id=?", String.class, cupom))
        .isEqualTo("UTILIZADO");
  }

  /**
   * A RN0035 permite "<b>um</b> pagamento de um valor menor que R$ 10,00 no cartão". Dois cartões
   * abaixo do mínimo extrapolam a exceção, ainda que haja cupons na composição.
   */
  @Test
  void RN0035_recusaDoisCartoesAbaixoDoMinimoComCupons() throws Exception {
    adicionar(1L, 1);
    long c1 = criarCartao();
    long c2 = criarCartao();
    BigDecimal total = resumoTotal(ENDERECO, null);
    BigDecimal noCartao = new BigDecimal("5.00");
    BigDecimal noCupom = total.subtract(noCartao).subtract(noCartao);
    long cupom = inserirCupomTroca(noCupom);
    Map<String, Object> payload =
        Map.of(
            "enderecoId", ENDERECO,
            "cuponsTroca", List.of(cupom),
            "cartoes",
                List.of(
                    Map.of("cartaoId", c1, "valor", noCartao),
                    Map.of("cartaoId", c2, "valor", noCartao)));
    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CARTAO_MINIMO"));
    assertThat(jdbc.queryForObject("select count(*) from pedido", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("select situacao from cupom where id=?", String.class, cupom))
        .isEqualTo("ATIVO");
  }

  // ---- RN0036: cupons superam o total e geram cupom de troco ----

  @Test
  void RN0036_cuponsSuperamTotalGeramCupomDeTroco() throws Exception {
    adicionar(1L, 1);
    BigDecimal total = resumoTotal(ENDERECO, null);
    BigDecimal cada =
        total.multiply(new BigDecimal("0.60")).setScale(2, java.math.RoundingMode.HALF_UP);
    long c1 = inserirCupomTroca(cada);
    long c2 = inserirCupomTroca(cada);
    BigDecimal troco = cada.add(cada).subtract(total);
    Map<String, Object> payload =
        Map.of("enderecoId", ENDERECO, "cuponsTroca", List.of(c1, c2), "cartoes", List.of());
    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.cupomTrocaGerado").doesNotExist());
    aprovarUltimoPedido();

    BigDecimal gerado =
        jdbc.queryForObject(
            "select valor from cupom where cliente_id=1 and origem like 'Troco%'",
            BigDecimal.class);
    assertThat(gerado).isEqualByComparingTo(troco);
  }

  @Test
  void RN0036_recusaCuponsDesnecessarios() throws Exception {
    adicionar(1L, 1);
    BigDecimal total = resumoTotal(ENDERECO, null);
    long c1 = inserirCupomTroca(total);
    long c2 = inserirCupomTroca(total);
    long c3 = inserirCupomTroca(total);
    Map<String, Object> payload =
        Map.of("enderecoId", ENDERECO, "cuponsTroca", List.of(c1, c2, c3), "cartoes", List.of());
    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CUPONS_EXCEDENTES"));
  }

  /**
   * Exemplo literal da RN0036: "a venda tem valor total de R$ 50,00 e o cliente possui três cupons,
   * um com valor de R$ 20,00, outro com valor de R$ 40,00 e um terceiro com valor de R$ 35,00 o
   * sistema não deve possibilitar o uso dos três cupons nesta compra, deve ser aceito apenas dois
   * cupons e consequentemente gerar um cupom com a diferença de R$ 5,00, ou R$ 10,00 ou R$ 25,00."
   * Cada par é aceito com o troco correspondente; o trio é recusado.
   */
  @ParameterizedTest(name = "{0} + {1} geram troco de {2}")
  @CsvSource({"20.00,35.00,5.00", "20.00,40.00,10.00", "40.00,35.00,25.00"})
  void RN0036_exemploDoDrsAceitaCadaParComTrocoDaDiferenca(String a, String b, String troco)
      throws Exception {
    compraDeCinquentaReais();
    long ca = inserirCupomTroca(new BigDecimal(a));
    long cb = inserirCupomTroca(new BigDecimal(b));
    Map<String, Object> payload =
        Map.of("enderecoId", ENDERECO, "cuponsTroca", List.of(ca, cb), "cartoes", List.of());
    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.total").value(50.00))
        .andExpect(jsonPath("$.cupomTrocaGerado").doesNotExist());
    aprovarUltimoPedido();
    assertThat(
            jdbc.queryForObject(
                "select valor from cupom where cliente_id=1 and origem like 'Troco%'",
                BigDecimal.class))
        .isEqualByComparingTo(troco);
  }

  @Test
  void RN0036_exemploDoDrsRecusaOsTresCupons() throws Exception {
    compraDeCinquentaReais();
    long c20 = inserirCupomTroca(new BigDecimal("20.00"));
    long c40 = inserirCupomTroca(new BigDecimal("40.00"));
    long c35 = inserirCupomTroca(new BigDecimal("35.00"));
    Map<String, Object> payload =
        Map.of("enderecoId", ENDERECO, "cuponsTroca", List.of(c20, c40, c35), "cartoes", List.of());
    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CUPONS_EXCEDENTES"));
    assertThat(jdbc.queryForObject("select count(*) from pedido", Integer.class)).isZero();
  }

  // ---- RN0023/RN0024/RN0025: composição do endereço e do cartão informados na compra ----

  /**
   * RN0023 exige os nove campos do endereço também quando o endereço é informado na compra —
   * inclusive quando o cliente opta por <b>não</b> incorporá-lo ao perfil.
   */
  @ParameterizedTest(name = "incorporar={0}")
  @ValueSource(booleans = {true, false})
  void RN0023_novoEnderecoNoCheckoutExigeCamposObrigatorios(boolean incorporar) throws Exception {
    adicionar(1L, 1);
    Map<String, Object> semBairro = new LinkedHashMap<>(novoEndereco());
    semBairro.put("bairro", "   ");
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("novoEndereco", semBairro);
    payload.put("incorporarEndereco", incorporar);
    payload.put("cartoes", List.of(Map.of("cartaoId", criarCartao(), "valor", "50.00")));
    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
    assertThat(jdbc.queryForObject("select count(*) from pedido", Integer.class)).isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from endereco where cliente_id=1 and apelido='Trabalho'",
                Integer.class))
        .isZero();
  }

  /**
   * RN0024 (composição do cartão) e RN0025 (bandeira registrada) valem para o cartão informado na
   * compra nos dois caminhos: incorporando ou não ao perfil.
   */
  @ParameterizedTest(name = "incorporar={0}")
  @ValueSource(booleans = {true, false})
  void RN0024_RN0025_novoCartaoNoCheckoutValidaComposicaoEBandeira(boolean incorporar)
      throws Exception {
    adicionar(1L, 1);
    BigDecimal total = resumoTotal(ENDERECO, null);

    // RN0024: número que não passa no dígito verificador
    recusaCartaoNovo(
        incorporar, cartaoNovo("4111111111111112", "123", 1), total, "CARTAO_INVALIDO");
    // RN0024: código de segurança fora do formato
    recusaCartaoNovo(
        incorporar, cartaoNovo("4111111111111111", "12", 1), total, "CODIGO_SEGURANCA_INVALIDO");
    // RN0025: bandeira inexistente no sistema
    recusaCartaoNovo(
        incorporar, cartaoNovo("4111111111111111", "123", 999), total, "BANDEIRA_INVALIDA");

    assertThat(jdbc.queryForObject("select count(*) from cartao where cliente_id=1", Integer.class))
        .isZero();
  }

  // ---- RN0033: no máximo um cupom promocional por compra ----

  /**
   * A RN0033 é garantida pelo contrato: {@code cupomPromocional} é um único código, então mais de
   * um cupom promocional por compra é impossível de representar — uma lista é recusada — e o pedido
   * registra exatamente um cupom promocional.
   */
  @Test
  void RN0033_apenasUmCupomPromocionalPorCompra() throws Exception {
    adicionar(1L, 1);
    long cartao = criarCartao();
    String promo = inserirCupomPromocional(new BigDecimal("10.00"));
    String outro = inserirCupomPromocional(new BigDecimal("5.00"));

    mvc.perform(
            post(base() + "/pedidos")
                .contentType(JSON)
                .content(
                    "{\"enderecoId\":1,\"cupomPromocional\":[\""
                        + promo
                        + "\",\""
                        + outro
                        + "\"],\"cartoes\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("FORMATO_INVALIDO"));

    BigDecimal comPromo = resumoTotal(ENDERECO, promo);
    Map<String, Object> payload =
        Map.of(
            "enderecoId", ENDERECO,
            "cupomPromocional", promo,
            "cartoes", List.of(Map.of("cartaoId", cartao, "valor", comPromo)));
    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isCreated());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from pedido where cupom_promocional_id is not null",
                Integer.class))
        .isEqualTo(1);
    // O cupom promocional entra como desconto do pedido, não como parcela de pagamento
    assertThat(
            jdbc.queryForObject(
                "select count(*) from pagamento_parte where tipo not in ('CARTAO','CUPOM_TROCA')",
                Integer.class))
        .isZero();
  }

  // ---- RNF0012: log de escrita com os dados alterados ----

  /**
   * RNF0012 exige "manter os dados alterados". A alteração de quantidade precisa guardar a
   * quantidade anterior, não apenas a resultante.
   */
  @Test
  void RNF0012_auditoriaDoCarrinhoGuardaAntesEDepois() throws Exception {
    adicionar(1L, 2);
    alterarQuantidade(itemIdDoProduto(1L), 5);
    Map<String, Object> registro =
        jdbc.queryForMap(
            "select dados_anteriores::text as antes, dados_novos::text as depois, ator, ocorrida_em"
                + " from auditoria_cliente where operacao='ALTERAR_ITEM_CARRINHO'");
    assertThat(registro.get("antes").toString()).contains("\"quantidade\": 2");
    assertThat(registro.get("depois").toString()).contains("\"quantidade\": 5");
    assertThat(registro.get("ator")).isNotNull();
    assertThat(registro.get("ocorrida_em")).isNotNull();

    remover(itemIdDoProduto(1L));
    assertThat(
            jdbc.queryForObject(
                "select dados_anteriores::text from auditoria_cliente"
                    + " where operacao='REMOVER_ITEM_CARRINHO'",
                String.class))
        .contains("\"quantidade\": 5");
  }

  // ---- RN0033: um cupom promocional aplica desconto ----

  @Test
  void RN0033_cupomPromocionalAplicaDesconto() throws Exception {
    adicionar(1L, 1);
    long cartao = criarCartao();
    String promo = inserirCupomPromocional(new BigDecimal("10.00"));
    BigDecimal semPromo = resumoTotal(ENDERECO, null);
    BigDecimal comPromo = resumoTotal(ENDERECO, promo);
    assertThat(comPromo).isLessThan(semPromo);

    Map<String, Object> payload =
        Map.of(
            "enderecoId", ENDERECO,
            "cupomPromocional", promo,
            "cartoes", List.of(Map.of("cartaoId", cartao, "valor", comPromo)));
    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("EM_PROCESSAMENTO"));
    assertThat(jdbc.queryForObject("select desconto from pedido", BigDecimal.class))
        .isEqualByComparingTo("8.99"); // 10% de 89.90
  }

  private void aprovarUltimoPedido() throws Exception {
    long pedido = jdbc.queryForObject("select max(id) from pedido", Long.class);
    mvc.perform(
            post("/api/v1/admin/pedidos/" + pedido + "/pagamento/validar")
                .contentType(JSON)
                .content("{\"resultadoSimulado\":\"APROVADO\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APROVADA"));
  }

  // ---- helpers ----

  private Map<String, Object> novoEndereco() {
    Map<String, Object> endereco = new LinkedHashMap<>();
    endereco.put("apelido", "Trabalho");
    endereco.put("tipoResidencia", "Comercial");
    endereco.put("tipoLogradouro", "Avenida");
    endereco.put("logradouro", "Paulista");
    endereco.put("numero", "1000");
    endereco.put("bairro", "Bela Vista");
    endereco.put("cep", "01310100");
    endereco.put("cidade", "São Paulo");
    endereco.put("estado", "SP");
    endereco.put("pais", "Brasil");
    endereco.put("observacoes", null);
    endereco.put("complemento", "Conjunto 10");
    endereco.put("cobranca", false);
    endereco.put("entrega", true);
    endereco.put("preferencialEntrega", false);
    return endereco;
  }

  private Map<String, Object> cartaoNovo(String numero, String cvv, long bandeiraId) {
    return Map.of(
        "numero", numero,
        "titular", "MARINA COSTA",
        "bandeiraId", bandeiraId,
        "codigoSeguranca", cvv,
        "preferencial", false);
  }

  private void recusaCartaoNovo(
      boolean incorporar, Map<String, Object> cartao, BigDecimal total, String codigoEsperado)
      throws Exception {
    Map<String, Object> payload =
        Map.of(
            "enderecoId",
            ENDERECO,
            "cartoes",
            List.of(
                Map.of(
                    "novoCartao", cartao,
                    "incorporarCartao", incorporar,
                    "valor", total)));
    mvc.perform(
            post(base() + "/pedidos").contentType(JSON).content(json.writeValueAsString(payload)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value(codigoEsperado));
  }

  /** Monta o valor total de R$ 50,00 usado no exemplo literal da RN0036. */
  private void compraDeCinquentaReais() throws Exception {
    // Produto de R$ 33,00 + frete SP de R$ 15,00 + R$ 2,00 por unidade = R$ 50,00.
    jdbc.update("update produto set preco=33.00 where id=1");
    adicionar(1L, 1);
    assertThat(resumoTotal(ENDERECO, null)).isEqualByComparingTo("50.00");
  }

  private String base() {
    return "/api/v1/clientes/" + CLIENTE;
  }

  private Map<String, Object> adicionar(long produtoId, int quantidade) throws Exception {
    String resp =
        mvc.perform(
                post(base() + "/carrinho/itens")
                    .contentType(JSON)
                    .content("{\"produtoId\":" + produtoId + ",\"quantidade\":" + quantidade + "}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readValue(resp, MAPA_JSON);
  }

  private Map<String, Object> alterarQuantidade(long itemId, int quantidade) throws Exception {
    String resp =
        mvc.perform(
                put(base() + "/carrinho/itens/" + itemId)
                    .contentType(JSON)
                    .content("{\"quantidade\":" + quantidade + "}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readValue(resp, MAPA_JSON);
  }

  private Map<String, Object> remover(long itemId) throws Exception {
    String resp =
        mvc.perform(delete(base() + "/carrinho/itens/" + itemId))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readValue(resp, MAPA_JSON);
  }

  private int unidades(Map<String, Object> carrinho) {
    return ((Number) carrinho.get("totalUnidades")).intValue();
  }

  private Long itemIdDoProduto(long produtoId) {
    return jdbc.queryForObject(
        "select ic.id from item_carrinho ic join carrinho c on c.id=ic.carrinho_id"
            + " where c.cliente_id=1 and ic.produto_id=?",
        Long.class,
        produtoId);
  }

  private long criarCartao() throws Exception {
    String resp =
        mvc.perform(
                post(base() + "/cartoes")
                    .contentType(JSON)
                    .content(
                        "{\"numero\":\"4111111111111111\",\"titular\":\"MARINA COSTA\",\"bandeiraId\":1,\"codigoSeguranca\":\"123\",\"preferencial\":false}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) json.readValue(resp, MAPA_JSON).get("id")).longValue();
  }

  private BigDecimal resumoTotal(Long enderecoId, String promo) throws Exception {
    return resumoTotal(enderecoId, null, promo);
  }

  private BigDecimal resumoTotal(Long enderecoId, String estado, String promo) throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    if (enderecoId != null) body.put("enderecoId", enderecoId);
    if (estado != null) body.put("estado", estado);
    if (promo != null) body.put("cupomPromocional", promo);
    String resp =
        mvc.perform(
                post(base() + "/carrinho/resumo")
                    .contentType(JSON)
                    .content(json.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new BigDecimal(json.readTree(resp).get("total").asText());
  }

  private long inserirCupomTroca(BigDecimal valor) {
    return jdbc.queryForObject(
        "insert into cupom(tipo,cliente_id,valor,situacao,criado_em) values"
            + " ('TROCA',1,?,'ATIVO', TIMESTAMPTZ '2026-09-01 12:00:00+00') returning id",
        Long.class,
        valor);
  }

  private String inserirCupomPromocional(BigDecimal percentual) {
    return jdbc.queryForObject(
        "insert into cupom(tipo,percentual,situacao,criado_em) values"
            + " ('PROMOCIONAL',?,'ATIVO', TIMESTAMPTZ '2026-09-01 12:00:00+00') returning codigo",
        String.class,
        percentual);
  }
}
