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
    var http = HttpClient.newHttpClient();
    var linhas = new ArrayList<String>();
    linhas.add("consulta,execucao,milissegundos");
    double max = 0;
    for (String q : List.of("", "?nome=Carga&ativo=true", "?codigo=CLI-000001")) {
      var request =
          HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/clientes" + q))
              .GET()
              .build();
      for (int i = 0; i < 5; i++)
        assertThat(http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode())
            .isEqualTo(200);
      for (int i = 0; i < 30; i++) {
        long inicio = System.nanoTime();
        var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        double ms = (System.nanoTime() - inicio) / 1e6;
        max = Math.max(max, ms);
        linhas.add(q + "," + i + "," + ms);
        assertThat(response.statusCode()).isEqualTo(200);
      }
    }
    Files.createDirectories(Path.of("target/evidencias"));
    Files.write(Path.of("target/evidencias/performance.csv"), linhas);
    assertThat(max)
        .as("Maior tempo HTTP após aquecimento, 1.000 clientes")
        .isLessThanOrEqualTo(1000);
  }
}
