package br.com.auralab;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/** A real, isolated database. Missing Docker fails explicitly. */
public abstract class PostgresTest {

  @org.springframework.beans.factory.annotation.Autowired
  protected org.springframework.jdbc.core.JdbcTemplate jdbc;

  protected static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine");

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @org.junit.jupiter.api.BeforeEach
  public void resetDatabase() throws Exception {
    try (var con = jdbc.getDataSource().getConnection()) {
      if (!con.getMetaData().getURL().equals(POSTGRES.getJdbcUrl()))
        throw new IllegalStateException("Refusing to reset a non-test database");
    }
    jdbc.execute(
        "truncate pagamento_parte, item_pedido, pedido, item_carrinho, carrinho, cupom,"
            + " auditoria_produto, produto, categoria_ativacao_produto, categoria_inativacao_produto,"
            + " auditoria_cliente, transacao_cliente, cartao, endereco, cliente, bandeira restart identity cascade");
    jdbc.execute(
        "insert into bandeira(nome) values ('Visa'),('Mastercard'),('Elo'),('American Express')");
    jdbc.execute(
        "insert into categoria_inativacao_produto(codigo, descricao) values"
            + " ('FORA_DE_MERCADO','Produto fora de mercado ou fora da linha comercial'),"
            + " ('DESCONTINUADO','Produto descontinuado sem previsão de reposição'),"
            + " ('ERRO_CADASTRO','Cadastro com erro que exige correção antes da venda')");
    jdbc.execute(
        "insert into categoria_ativacao_produto(codigo, descricao) values"
            + " ('RETORNO_AO_MERCADO','Produto retorna ao mercado após período indisponível'),"
            + " ('REPOSICAO_ESTOQUE','Produto reativado com reposição de estoque'),"
            + " ('CORRECAO_CADASTRO','Produto reativado após correção cadastral')");
    for (String migration :
        java.util.List.of(
            "V2__massa_demonstracao.sql",
            "V4__historico_demonstrativo.sql")) {
      String sql =
          new String(
              getClass().getResourceAsStream("/db/migration/" + migration).readAllBytes(),
              java.nio.charset.StandardCharsets.UTF_8);
      jdbc.execute(sql.replace("current_timestamp", "TIMESTAMPTZ '2026-09-01 12:00:00+00'"));
    }
    jdbc.execute(
        "update transacao_cliente set status='PAGAMENTO_REALIZADO' where tipo='PEDIDO' and status='APROVADA'");
    jdbc.execute("insert into produto(nome,marca,descricao,imagem_url,preco,estoque,ativo,visivel,criado_em,atualizado_em) values"
        + " ('Creme Facial Vitamina C','Aura Derma','Creme facial hidratante com vitamina C.','https://example.com/creme.png',89.90,25,true,true,current_timestamp,current_timestamp),"
        + " ('Sérum Ácido Hialurônico','Aura Derma','Sérum facial hidratante.','https://example.com/serum.png',129.90,12,true,true,current_timestamp,current_timestamp)");
    jdbc.execute("insert into produto(nome,marca,descricao,imagem_url,preco,estoque,ativo,visivel,categoria_inativacao,justificativa_inativacao,inativado_em,criado_em,atualizado_em) values"
        + " ('Protetor Solar FPS 60','Solar Lab','Protetor solar facial FPS 60.','https://example.com/protetor.png',59.90,0,false,false,'FORA_DE_MERCADO','Linha saiu de catálogo após esgotar o último lote.',current_timestamp,current_timestamp,current_timestamp)");
    jdbc.execute("insert into produto_categoria(produto_id,categoria_codigo) select id,'FACE' from produto");
    jdbc.execute("insert into produto_categoria(produto_id,categoria_codigo) select id,'SKINCARE' from produto");
    jdbc.execute("insert into produto_categoria(produto_id,categoria_codigo) select id,'HIDRATANTES' from produto where id in (1,2)");
  }
}
