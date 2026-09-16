package br.com.auralab.clientes;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.auralab.PostgresTest;
import br.com.auralab.clientes.api.dto.*;
import br.com.auralab.clientes.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class ClienteIntegridadeTest extends PostgresTest {
  @Autowired ClienteService clientes;
  @Autowired CartaoService cartoes;
  @Autowired EnderecoService enderecos;
  @Autowired ObjectMapper json;
  @Autowired PasswordEncoder encoder;
  @MockitoSpyBean AuditoriaService auditoria;

  static ClienteInput novo(String email, String cpf) {
    return new ClienteInput(
        "Cliente Teste",
        cpf,
        email,
        "Feminino",
        java.time.LocalDate.of(1990, 1, 1),
        "Celular",
        "11",
        "988887777",
        "Senha@123",
        "Senha@123",
        endereco("Casa", true, true));
  }

  static EnderecoInput endereco(String apelido, boolean cobranca, boolean entrega) {
    return new EnderecoInput(
        apelido,
        "Casa",
        "Rua",
        "Flores",
        "12",
        "Centro",
        "01001000",
        "São Paulo",
        "SP",
        "Brasil",
        "Observação",
        "Apto 2",
        cobranca,
        entrega,
        false);
  }

  static CartaoInput cartao(boolean preferencial) {
    return new CartaoInput("4111111111111111", "TITULAR TESTE", 1L, "123", preferencial);
  }

  @Test
  void RNF0012_falhaAuditoriaReverteCadastro() {
    doThrow(new IllegalStateException("Falha simulada"))
        .when(alvoAuditoria())
        .registrar(anyLong(), eq("CADASTRAR_CLIENTE"), anyString(), anyLong(), isNull(), anyMap());
    try {
      assertThatThrownBy(() -> clientes.cadastrar(novo("rollback@example.com", "11144477735")))
          .isInstanceOf(IllegalStateException.class);
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from cliente where email='rollback@example.com'", Integer.class))
          .isZero();
      assertThat(jdbc.queryForObject("select count(*) from endereco", Integer.class)).isEqualTo(2);
    } finally {
      reset(alvoAuditoria());
    }
  }

  @Test
  void RNF0012_falhaAuditoriaRevertePreferencia() {
    long primeiro = cartoes.adicionar(1L, cartao(false)).id();
    long segundo = cartoes.adicionar(1L, cartao(false)).id();
    doThrow(new IllegalStateException("Falha simulada"))
        .when(alvoAuditoria())
        .registrar(
            anyLong(),
            eq("DEFINIR_CARTAO_PREFERENCIAL"),
            anyString(),
            anyLong(),
            anyMap(),
            anyMap());
    try {
      assertThatThrownBy(() -> cartoes.preferencial(1L, segundo))
          .isInstanceOf(IllegalStateException.class);
      assertThat(
              cartoes.listar(1L).stream()
                  .filter(CartaoView::preferencial)
                  .findFirst()
                  .orElseThrow()
                  .id())
          .isEqualTo(primeiro);
    } finally {
      reset(alvoAuditoria());
    }
  }

  @Test
  void RNF0033_hashESegredosNaoPersistidos() throws Exception {
    var c = clientes.cadastrar(novo("seguro@example.com", "11144477735"));
    clientes.alterarSenha(c.id(), new SenhaInput("NovaSenha@123", "NovaSenha@123"));
    cartoes.adicionar(c.id(), cartao(false));
    String hash =
        jdbc.queryForObject("select senha_hash from cliente where id=?", String.class, c.id());
    assertThat(encoder.matches("NovaSenha@123", hash)).isTrue();
    assertThat(encoder.matches("Senha@123", hash)).isFalse();
    String dados =
        json.writeValueAsString(auditoria.listar(c.id()))
            + json.writeValueAsString(clientes.buscar(c.id()))
            + jdbc.queryForObject(
                "select row_to_json(c)::text from cartao c where cliente_id=?",
                String.class,
                c.id());
    assertThat(dados)
        .doesNotContain(
            "Senha@123", "NovaSenha@123", hash, "4111111111111111", "codigoSeguranca", "senhaHash");
  }

  @Test
  void RF0027_preferenciaConcorrente() throws Exception {
    long a = cartoes.adicionar(1L, cartao(false)).id(),
        b = cartoes.adicionar(1L, cartao(false)).id();
    juntos(
        () -> {
          cartoes.preferencial(1L, a);
          return 1;
        },
        () -> {
          cartoes.preferencial(1L, b);
          return 1;
        });
    assertThat(cartoes.listar(1L).stream().filter(c -> c.ativo() && c.preferencial()).count())
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auditoria_cliente where operacao='DEFINIR_CARTAO_PREFERENCIAL'",
                Integer.class))
        .isEqualTo(2);
  }

  @Test
  void RF0021_unicidadeConcorrente() throws Exception {
    Callable<Integer> tarefa =
        () -> {
          try {
            clientes.cadastrar(novo("unico@example.com", "11144477735"));
            return 1;
          } catch (RegraNegocioException
              | org.springframework.dao.DataIntegrityViolationException e) {
            return 0;
          }
        };
    assertThat(juntos(tarefa, tarefa).stream().mapToInt(Integer::intValue).sum()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from cliente where email='unico@example.com'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void RF0026_associacaoEEnderecoInativo() {
    var e = enderecos.adicionar(1L, endereco("Segundo", true, true));
    assertThat(e.id()).isNotNull();
    assertThatThrownBy(() -> enderecos.alterar(2L, e.id(), endereco("Outro", true, true)))
        .isInstanceOf(RegraNegocioException.class);
    enderecos.inativar(1L, e.id());
    assertThatThrownBy(() -> enderecos.alterar(1L, e.id(), endereco("Outro", true, true)))
        .isInstanceOf(RegraNegocioException.class);
  }

  @Test
  void RN0027_naoContaPagamentoTrocaOuCupom() {
    assertThat(clientes.buscar(1L).ranking()).isEqualTo(4);
  }

  private List<Integer> juntos(Callable<Integer> a, Callable<Integer> b) throws Exception {
    try (var pool = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      var f1 =
          pool.submit(
              () -> {
                start.await();
                return a.call();
              });
      var f2 =
          pool.submit(
              () -> {
                start.await();
                return b.call();
              });
      start.countDown();
      return List.of(f1.get(20, TimeUnit.SECONDS), f2.get(20, TimeUnit.SECONDS));
    }
  }

  private AuditoriaService alvoAuditoria() {
    return org.springframework.test.util.AopTestUtils.getUltimateTargetObject(auditoria);
  }
}
