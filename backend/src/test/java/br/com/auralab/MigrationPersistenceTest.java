package br.com.auralab;

import static org.assertj.core.api.Assertions.*;

import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

class MigrationPersistenceTest {
  @Test
  void reiniciarPreservaSituacaoEMotivoDeProduto() throws Exception {
    try (var pg = new PostgreSQLContainer<>("postgres:17-alpine")) {
      pg.start();
      Flyway.configure()
          .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
          .load()
          .migrate();
      Map<String, Object> props =
          Map.of(
              "spring.datasource.url",
              pg.getJdbcUrl(),
              "spring.datasource.username",
              pg.getUsername(),
              "spring.datasource.password",
              pg.getPassword(),
              "server.port",
              0);
      try (var app =
          new SpringApplicationBuilder(AuraLabApplication.class).properties(props).run()) {
        var service = app.getBean(br.com.auralab.produtos.service.ProdutoService.class);
        assertThat(service.buscar(3L).ativo()).isFalse();
        service.ativar(
            3L,
            new br.com.auralab.produtos.api.dto.MotivoStatusProdutoInput(
                "RETORNO_AO_MERCADO", "Novo lote disponível para venda."));
      }
      try (var app =
          new SpringApplicationBuilder(AuraLabApplication.class).properties(props).run()) {
        var service = app.getBean(br.com.auralab.produtos.service.ProdutoService.class);
        var v = service.buscar(3L);
        assertThat(v.ativo()).isTrue();
        assertThat(v.visivel()).isFalse();
        assertThat(v.categoriaAtivacao()).isEqualTo("RETORNO_AO_MERCADO");
        assertThat(v.justificativaAtivacao()).contains("Novo lote");
      }
    }
  }

  @Test
  void migrarV2PreservaLegadoEReiniciarPreservaDados() throws Exception {
    try (var pg = new PostgreSQLContainer<>("postgres:17-alpine")) {
      pg.start();
      Flyway.configure()
          .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
          .target("2")
          .load()
          .migrate();
      try (var c =
              DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
          var st = c.createStatement()) {
        st.execute(
            "insert into auditoria_cliente(cliente_id,ocorrida_em,ator,operacao,alteracoes) values(1,current_timestamp,'ANTIGO','ALTERAR_CLIENTE','Registro anterior sem snapshot')");
      }
      Flyway.configure()
          .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
          .load()
          .migrate();
      Map<String, Object> props =
          Map.of(
              "spring.datasource.url",
              pg.getJdbcUrl(),
              "spring.datasource.username",
              pg.getUsername(),
              "spring.datasource.password",
              pg.getPassword(),
              "server.port",
              0);
      try (var app =
          new SpringApplicationBuilder(AuraLabApplication.class).properties(props).run()) {
        var service = app.getBean(br.com.auralab.clientes.service.ClienteService.class);
        assertThat(service.buscar(1L).codigo()).isEqualTo("CLI-000001");
        service.inativar(1L);
      }
      try (var app =
          new SpringApplicationBuilder(AuraLabApplication.class).properties(props).run()) {
        var service = app.getBean(br.com.auralab.clientes.service.ClienteService.class);
        assertThat(service.buscar(1L).ativo()).isFalse();
        assertThat(service.buscar(1L).enderecos()).hasSize(1);
        var aud = app.getBean(br.com.auralab.clientes.service.AuditoriaService.class).listar(1L);
        assertThat(aud)
            .anySatisfy(
                a -> {
                  assertThat(a.getEntidade()).isEqualTo("LEGADO");
                  assertThat(a.getDadosAnteriores()).isNull();
                  assertThat(a.getAlteracoes()).isEqualTo("Registro anterior sem snapshot");
                });
      }
    }
  }
}
