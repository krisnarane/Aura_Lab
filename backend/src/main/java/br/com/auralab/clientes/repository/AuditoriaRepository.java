package br.com.auralab.clientes.repository;

import br.com.auralab.clientes.domain.AuditoriaCliente;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<AuditoriaCliente, Long> {
  List<AuditoriaCliente> findByClienteIdOrderByOcorridaEmDesc(Long id);
}
