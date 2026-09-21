package br.com.auralab.clientes.repository;

import br.com.auralab.clientes.domain.AuditoriaCliente;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório da trilha de auditoria de clientes (RNF0012); a consulta devolve o histórico mais recente primeiro. */
public interface AuditoriaRepository extends JpaRepository<AuditoriaCliente, Long> {
  List<AuditoriaCliente> findByClienteIdOrderByOcorridaEmDesc(Long id);
}
