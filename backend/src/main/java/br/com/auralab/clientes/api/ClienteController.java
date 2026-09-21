package br.com.auralab.clientes.api;

import br.com.auralab.clientes.api.dto.*;
import br.com.auralab.clientes.service.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST do módulo de clientes (camada MVC). Apenas delega aos services —
 * controllers não acessam repositories; o vínculo dos recursos filhos ao cliente da URL é
 * validado no service.
 *
 * <p>Endpoints cobrem RF0021 a RF0028: CRUD e inativação de cliente, senha isolada,
 * endereços, cartões, transações demonstrativas, auditoria e consulta de bandeiras.
 */
@RestController
@RequestMapping("/api/v1")
public class ClienteController {
  private final ClienteService clientes;
  private final EnderecoService enderecos;
  private final CartaoService cartoes;
  private final TransacaoService transacoes;
  private final AuditoriaService auditoria;

  public ClienteController(
      ClienteService c,
      EnderecoService e,
      CartaoService ca,
      TransacaoService t,
      AuditoriaService a) {
    clientes = c;
    enderecos = e;
    cartoes = ca;
    transacoes = t;
    auditoria = a;
  }

  @PostMapping("/clientes")
  ResponseEntity<ClienteView> cadastrar(@Valid @RequestBody ClienteInput in) {
    var v = clientes.cadastrar(in);
    return ResponseEntity.created(URI.create("/api/v1/clientes/" + v.id())).body(v);
  }

  /** RF0024: consulta com filtros isolados ou combinados por todos os campos de identificação. */
  @GetMapping("/clientes")
  List<ClienteView> listar(
      @RequestParam(required = false) String codigo,
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) String cpf,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String genero,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate nascimento,
      @RequestParam(required = false) String telefoneTipo,
      @RequestParam(required = false) String telefoneDdd,
      @RequestParam(required = false) String telefoneNumero,
      @RequestParam(required = false) Boolean ativo) {
    return clientes.listar(
        codigo,
        nome,
        cpf,
        email,
        genero,
        nascimento,
        telefoneTipo,
        telefoneDdd,
        telefoneNumero,
        ativo);
  }

  @GetMapping("/clientes/{id}")
  ClienteView buscar(@PathVariable Long id) {
    return clientes.buscar(id);
  }

  @PutMapping("/clientes/{id}")
  ClienteView alterar(@PathVariable Long id, @Valid @RequestBody ClienteAlteracaoInput in) {
    return clientes.alterar(id, in);
  }

  /** RF0023: inativação lógica (não é exclusão física). */
  @PatchMapping("/clientes/{id}/inativacao")
  ClienteView inativar(@PathVariable Long id) {
    return clientes.inativar(id);
  }

  /** RF0028: alteração apenas de senha, endpoint independente dos dados cadastrais. */
  @PutMapping("/clientes/{id}/senha")
  ResponseEntity<Void> senha(@PathVariable Long id, @Valid @RequestBody SenhaInput in) {
    clientes.alterarSenha(id, in);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/clientes/{id}/enderecos")
  List<EnderecoView> enderecos(@PathVariable Long id) {
    return enderecos.listar(id);
  }

  @PostMapping("/clientes/{id}/enderecos")
  ResponseEntity<EnderecoView> adicionarEndereco(
      @PathVariable Long id, @Valid @RequestBody EnderecoInput in) {
    return ResponseEntity.status(201).body(enderecos.adicionar(id, in));
  }

  @PutMapping("/clientes/{id}/enderecos/{enderecoId}")
  EnderecoView alterarEndereco(
      @PathVariable Long id, @PathVariable Long enderecoId, @Valid @RequestBody EnderecoInput in) {
    return enderecos.alterar(id, enderecoId, in);
  }

  @PatchMapping("/clientes/{id}/enderecos/{enderecoId}/inativacao")
  ResponseEntity<Void> inativarEndereco(@PathVariable Long id, @PathVariable Long enderecoId) {
    enderecos.inativar(id, enderecoId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/clientes/{id}/cartoes")
  List<CartaoView> cartoes(@PathVariable Long id) {
    return cartoes.listar(id);
  }

  @PostMapping("/clientes/{id}/cartoes")
  ResponseEntity<CartaoView> adicionarCartao(
      @PathVariable Long id, @Valid @RequestBody CartaoInput in) {
    return ResponseEntity.status(201).body(cartoes.adicionar(id, in));
  }

  @PutMapping("/clientes/{id}/cartoes/{cartaoId}/preferencial")
  ResponseEntity<Void> preferencial(@PathVariable Long id, @PathVariable Long cartaoId) {
    cartoes.preferencial(id, cartaoId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/clientes/{id}/cartoes/{cartaoId}/inativacao")
  ResponseEntity<Void> inativarCartao(@PathVariable Long id, @PathVariable Long cartaoId) {
    cartoes.inativar(id, cartaoId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/clientes/{id}/transacoes")
  List<TransacaoView> transacoes(@PathVariable Long id) {
    return transacoes.listar(id);
  }

  @GetMapping("/clientes/{id}/transacoes/{transacaoId}")
  TransacaoView transacao(@PathVariable Long id, @PathVariable Long transacaoId) {
    return transacoes.buscar(id, transacaoId);
  }

  /** RNF0012: histórico da trilha gravada em cada escrita. */
  @GetMapping("/clientes/{id}/auditoria")
  List<AuditoriaView> auditoria(@PathVariable Long id) {
    clientes.buscar(id);
    return auditoria.listar(id).stream()
        .map(
            a ->
                new AuditoriaView(
                    a.getId(),
                    a.getOcorridaEm(),
                    a.getAtor(),
                    a.getOperacao(),
                    a.getAlteracoes(),
                    a.getEntidade(),
                    a.getEntidadeId(),
                    a.getDadosAnteriores(),
                    a.getDadosNovos()))
        .toList();
  }

  /** RN0025: domínio de bandeiras cadastradas para preencher o formulário de cartão. */
  @GetMapping("/bandeiras")
  List<BandeiraView> bandeiras() {
    return cartoes.listarBandeiras();
  }
}
