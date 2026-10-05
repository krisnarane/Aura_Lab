package br.com.auralab;

import static org.assertj.core.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsultaPerformanceTest extends PostgresTest {
  @LocalServerPort int port;

  @Test
  void RNF0011_consultasAteUmSegundo() throws Exception {
    jdbc.execute(
        "insert into cliente(nome,cpf,email,genero,nascimento,telefone_tipo,telefone_ddd,telefone_numero,senha_hash,ativo,criado_em,atualizado_em) select 'Carga '||n,lpad((90000000000+n)::text,11,'0'),'carga'||n||'@example.com','Outro','1990-01-01','Celular','11','988887777','hash-fixture',true,current_timestamp,current_timestamp from generate_series(1,998) n");
    jdbc.execute(
        "insert into endereco(cliente_id,apelido,tipo_residencia,tipo_logradouro,logradouro,numero,bairro,cep,cidade,estado,pais,cobranca,entrega,preferencial_entrega,ativo) select id,'Casa','Casa','Rua','Carga','1','Centro','01001000','São Paulo','SP','Brasil',true,true,true,true from cliente where id>2");
    var linhas = novaPlanilha();
    double max = 0;
    for (String q : List.of("", "?nome=Carga&ativo=true", "?codigo=CLI-000001"))
      max = Math.max(max, medir("/clientes" + q, linhas));
    gravar("performance.csv", linhas);
    assertThat(max)
        .as("Maior tempo HTTP após aquecimento, 1.000 clientes")
        .isLessThanOrEqualTo(1000);
  }

  /**
   * RNF0011 aplicado às consultas do módulo de vendas: carrinho, histórico de pedidos, pedido
   * individual e cupons do cliente, com massa suficiente para que uma consulta por item aparecesse
   * na medição.
   */
  @Test
  void RNF0011_consultasDeVendasAteUmSegundo() throws Exception {
    jdbc.execute(
        "insert into carrinho(cliente_id,criado_em,atualizado_em) values (1,current_timestamp,current_timestamp)");
    jdbc.execute(
        "insert into item_carrinho(carrinho_id,produto_id,quantidade) select c.id,p.id,2 from carrinho c, produto p where c.cliente_id=1 and p.id in (1,2)");
    jdbc.execute(
        "insert into cupom(tipo,cliente_id,valor,situacao,criado_em) select 'TROCA',1,25.00,'ATIVO',current_timestamp from generate_series(1,100)");
    jdbc.execute(
        "insert into pedido(cliente_id,status,subtotal,desconto,frete,total,entrega_tipo_residencia,entrega_tipo_logradouro,entrega_logradouro,entrega_numero,entrega_bairro,entrega_cep,entrega_cidade,entrega_estado,entrega_pais,criado_em)"
            + " select 1,'EM_PROCESSAMENTO',89.90,0,17.00,106.90,'Casa','Rua','das Flores','120','Centro','01310100','São Paulo','SP','Brasil',current_timestamp from generate_series(1,200)");
    jdbc.execute(
        "insert into item_pedido(pedido_id,produto_id,produto_codigo,produto_nome,preco_unitario,quantidade,subtotal) select id,1,'PRD-000001','Creme Facial Vitamina C',89.90,1,89.90 from pedido");
    jdbc.execute(
        "insert into pagamento_parte(pedido_id,tipo,valor,descricao) select id,'CARTAO',106.90,'Visa final 1111' from pedido");
    Long pedidoId = jdbc.queryForObject("select min(id) from pedido", Long.class);

    var linhas = novaPlanilha();
    double max = 0;
    for (String caminho :
        List.of(
            "/clientes/1/carrinho",
            "/clientes/1/pedidos",
            "/clientes/1/pedidos/" + pedidoId,
            "/clientes/1/cupons")) max = Math.max(max, medir(caminho, linhas));
    gravar("performance-vendas.csv", linhas);
    assertThat(max)
        .as("Maior tempo HTTP após aquecimento, 200 pedidos e 100 cupons")
        .isLessThanOrEqualTo(1000);
  }

  // ---- apoio ---------------------------------------------------------------------------------

  private ArrayList<String> novaPlanilha() {
    var linhas = new ArrayList<String>();
    linhas.add("consulta,execucao,milissegundos");
    return linhas;
  }

  /** Cinco execuções de aquecimento e trinta medidas, como na verificação original. */
  private double medir(String caminho, List<String> linhas) throws Exception {
    var http = HttpClient.newHttpClient();
    var request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + caminho))
            .GET()
            .build();
    for (int i = 0; i < 5; i++)
      assertThat(http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode())
          .isEqualTo(200);
    double max = 0;
    for (int i = 0; i < 30; i++) {
      long inicio = System.nanoTime();
      var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
      double ms = (System.nanoTime() - inicio) / 1e6;
      max = Math.max(max, ms);
      linhas.add(caminho + "," + i + "," + ms);
      assertThat(response.statusCode()).isEqualTo(200);
    }
    return max;
  }

  private void gravar(String arquivo, List<String> linhas) throws Exception {
    Files.createDirectories(Path.of("target/evidencias"));
    Files.write(Path.of("target/evidencias", arquivo), linhas);
  }
}
