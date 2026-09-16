package br.com.auralab.clientes;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.auralab.clientes.domain.Cliente;
import br.com.auralab.clientes.repository.ClienteRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClienteApiTest extends br.com.auralab.PostgresTest {
  @Autowired MockMvc mvc;
  @Autowired ClienteRepository repo;
  @Autowired PasswordEncoder encoder;
  private static final String JSON =
      """
   {"nome":"Joana Teste","cpf":"11144477735","email":"joana.teste@example.com","genero":"Feminino","nascimento":"1990-01-01","telefoneTipo":"Celular","telefoneDdd":"11","telefoneNumero":"988887777","senha":"Senha@123","confirmacaoSenha":"Senha@123","enderecoResidencial":{"apelido":"Casa","tipoResidencia":"Casa","tipoLogradouro":"Rua","logradouro":"Teste","numero":"10","bairro":"Centro","cep":"01001000","cidade":"São Paulo","estado":"SP","pais":"Brasil","observacoes":"","cobranca":true,"entrega":true,"preferencialEntrega":true}}
   """;

  @Test
  void RF0021_RN0026_RNF0033_deveCadastrarComCodigoEnderecoEHash() throws Exception {
    mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content(JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigo").isNotEmpty())
        .andExpect(jsonPath("$.enderecos[0].cobranca").value(true))
        .andExpect(jsonPath("$.enderecos[0].entrega").value(true));
    Cliente c =
        repo.findAll().stream()
            .filter(x -> x.getEmail().equals("joana.teste@example.com"))
            .findFirst()
            .orElseThrow();
    assertThat(c.getSenhaHash()).doesNotContain("Senha@123");
    assertThat(encoder.matches("Senha@123", c.getSenhaHash())).isTrue();
  }

  @Test
  void RNF0031_deveRecusarSenhaFracaSemPersistir() throws Exception {
    String fraca = JSON.replace("Senha@123", "senha");
    mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content(fraca))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("SENHA_FRACA"));
    assertThat(repo.existsByEmailIgnoreCase("joana.teste@example.com")).isFalse();
  }

  @Test
  void RF0024_deveCombinarFiltros() throws Exception {
    mvc.perform(get("/api/v1/clientes").param("nome", "Marina").param("ativo", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].codigo").value("CLI-000001"))
        .andExpect(jsonPath("$[1]").doesNotExist());
  }

  @Test
  void RF0023_deveInativarEManterCadastro() throws Exception {
    mvc.perform(patch("/api/v1/clientes/1/inativacao"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativo").value(false));
    assertThat(repo.findById(1L)).get().extracting(Cliente::isAtivo).isEqualTo(false);
  }

  @Test
  void contratosErrosEAssociacao() throws Exception {
    mvc.perform(get("/api/v1/clientes").param("nascimento", "nao-data"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("FORMATO_INVALIDO"));
    mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos").isMap());
    mvc.perform(get("/api/v1/clientes/2/transacoes/1")).andExpect(status().isNotFound());
    mvc.perform(
            post("/api/v1/clientes/1/cartoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"numero\":\"4111111111111111\",\"titular\":\"TESTE\",\"bandeiraId\":999,\"codigoSeguranca\":\"123\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.bandeiraId").exists());
  }
}
