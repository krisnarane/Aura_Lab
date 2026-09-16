package br.com.auralab.produtos.service;

import br.com.auralab.produtos.domain.AuditoriaProduto;
import br.com.auralab.produtos.repository.AuditoriaProdutoRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class AuditoriaProdutoService {
  private final AuditoriaProdutoRepository repo;

  public AuditoriaProdutoService(AuditoriaProdutoRepository repo) {
    this.repo = repo;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void registrar(
      Long produto,
      String operacao,
      String entidade,
      Long id,
      Map<String, Object> antes,
      Map<String, Object> depois) {
    repo.saveAndFlush(new AuditoriaProduto(produto, operacao, entidade, id, antes, depois));
  }
}
