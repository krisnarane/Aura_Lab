package br.com.auralab.clientes.service;

import br.com.auralab.clientes.api.dto.*;
import br.com.auralab.clientes.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service somente-leitura das transações demonstrativas (RF0025). O vínculo com o cliente
 * da URL é sempre validado antes de devolver o detalhe (404 se não pertencer).
 */
@Service
public class TransacaoService {
  private final ClienteRepository clientes;
  private final TransacaoRepository repo;

  public TransacaoService(ClienteRepository c, TransacaoRepository r) {
    clientes = c;
    repo = r;
  }

  @Transactional(readOnly = true)
  public List<TransacaoView> listar(Long id) {
    garantir(id);
    return repo.findByClienteIdOrderByOcorridaEmDesc(id).stream()
        .map(
            t ->
                new TransacaoView(
                    t.getId(),
                    t.getCodigo(),
                    t.getTipo(),
                    t.getStatus(),
                    t.getValor(),
                    t.getOcorridaEm(),
                    t.getDetalhes()))
        .toList();
  }

  @Transactional(readOnly = true)
  public TransacaoView buscar(Long c, Long id) {
    garantir(c);
    var t =
        repo.findByIdAndClienteId(id, c)
            .orElseThrow(
                () ->
                    new RegraNegocioException(
                        "TRANSACAO_NAO_ENCONTRADA", "Transação não encontrada para este cliente."));
    return new TransacaoView(
        t.getId(),
        t.getCodigo(),
        t.getTipo(),
        t.getStatus(),
        t.getValor(),
        t.getOcorridaEm(),
        t.getDetalhes());
  }

  private void garantir(Long id) {
    if (!clientes.existsById(id))
      throw new RegraNegocioException("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado.");
  }
}
