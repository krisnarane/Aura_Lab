package br.com.auralab;

import static org.assertj.core.api.Assertions.*;

import br.com.auralab.vendas.api.dto.*;
import br.com.auralab.vendas.service.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/** Mede a meta local de 2 s para RF0055; o RNF0011 do DRS ainda especifica 1 s. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TempoVendaTeste.class)
class AnaliseVendasPerformanceTest extends PostgresTest {
  @LocalServerPort int port;
  @Autowired CarrinhoService carrinho;
  @Autowired VendaService venda;
  @Autowired CicloPedidoService ciclo;
  @Autowired TempoVendaTeste.Relogio relogio;

  @Test void RF0055_RNF0011_consultaMilPedidosAteDoisSegundos() throws Exception {
    relogio.reiniciar();
    jdbc.update("update produto set estoque=1200 where id=1");
    long cartao = jdbc.queryForObject("insert into cartao(cliente_id,bandeira_id,titular,ultimos_quatro,preferencial,ativo) "
        + "values(1,1,'MARINA COSTA','1111',false,true) returning id", Long.class);
    carrinho.adicionar(1L, new ItemCarrinhoInput(1L, 1));
    var total = venda.resumo(1L, new ResumoInput(1L, null, null)).total();
    long pedido = venda.finalizar(1L, new CheckoutInput(1L, null, false, null, List.of(),
        List.of(new ParcelaCartaoInput(cartao, null, false, total)))).id();
    ciclo.validar(pedido);
    ciclo.despachar(pedido);
    ciclo.entregar(pedido);
    // Os clones são apenas massa de leitura da consulta; não alteram pedidos legados
    // nem simulam eventos de pagamento na aplicação de demonstração.
    jdbc.update("""
        insert into pedido(cliente_id,status,subtotal,desconto,frete,total,
          entrega_tipo_residencia,entrega_tipo_logradouro,entrega_logradouro,
          entrega_numero,entrega_bairro,entrega_cep,entrega_cidade,entrega_estado,
          entrega_pais,criado_em,legado,pagamento_status)
        select p.cliente_id,'ENTREGUE',p.subtotal,p.desconto,p.frete,p.total,
          p.entrega_tipo_residencia,p.entrega_tipo_logradouro,p.entrega_logradouro,
          p.entrega_numero,p.entrega_bairro,p.entrega_cep,p.entrega_cidade,p.entrega_estado,
          p.entrega_pais,p.criado_em + (n / 100) * interval '30 days',false,'APROVADO'
          from pedido p cross join generate_series(1,999) n where p.id=?
        """, pedido);
    jdbc.update("""
        insert into item_pedido(pedido_id,produto_id,produto_codigo,produto_nome,preco_unitario,quantidade,subtotal)
        select p.id,i.produto_id,i.produto_codigo,i.produto_nome,i.preco_unitario,i.quantidade,i.subtotal
          from pedido p cross join item_pedido i where i.pedido_id=? and p.id<>?
        """, pedido, pedido);
    assertThat(jdbc.queryForObject("select count(*) from pedido where status='ENTREGUE'", Integer.class)).isEqualTo(1000);
    var url = URI.create("http://localhost:" + port + "/api/v1/admin/analise-vendas?inicio=2026-10-01&fim=2027-12-31&categorias=FACE&categorias=SKINCARE");
    var request = HttpRequest.newBuilder(url).header("X-Demo-Perfil", "ADMIN_DEMO").GET().build();
    var http = HttpClient.newHttpClient();
    for (int n = 0; n < 15; n++) assertThat(http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200);
    var linhas = new ArrayList<String>();
    linhas.add("consulta,execucao,milissegundos,pedidos");
    double maior = 0;
    for (int n = 0; n < 30; n++) {
      long inicio = System.nanoTime();
      var resposta = http.send(request, HttpResponse.BodyHandlers.discarding());
      double ms = (System.nanoTime() - inicio) / 1e6;
      assertThat(resposta.statusCode()).isEqualTo(200);
      linhas.add("analise-vendas," + n + "," + ms + ",1000");
      maior = Math.max(maior, ms);
    }
    Files.createDirectories(Path.of("target/evidencias"));
    Files.write(Path.of("target/evidencias/performance-analise-vendas.csv"), linhas);
    assertThat(maior).as("Maior resposta HTTP após aquecimento, 1.000 pedidos; meta local de 2 s")
        .isLessThanOrEqualTo(2000);
  }
}
