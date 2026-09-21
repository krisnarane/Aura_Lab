package br.com.auralab.clientes.service;

import br.com.auralab.clientes.domain.*;
import br.com.auralab.clientes.repository.AuditoriaRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/**
 * Service de auditoria de clientes (RNF0012). A propagação MANDATORY garante que o
 * registro sempre acontece dentro da transação da operação auditada: se a escrita falhar,
 * a trilha falha junto.
 */
@Service
public class AuditoriaService {
  private final AuditoriaRepository repo;

  public AuditoriaService(AuditoriaRepository repo) {
    this.repo = repo;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void registrar(
      Long cliente,
      String operacao,
      String entidade,
      Long id,
      Map<String, Object> antes,
      Map<String, Object> depois) {
    repo.saveAndFlush(new AuditoriaCliente(cliente, operacao, entidade, id, antes, depois));
  }

  @Transactional(readOnly = true)
  public List<AuditoriaCliente> listar(Long id) {
    return repo.findByClienteIdOrderByOcorridaEmDesc(id);
  }
}
