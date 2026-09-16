package br.com.auralab.clientes.repository;

import br.com.auralab.clientes.domain.Bandeira;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BandeiraRepository extends JpaRepository<Bandeira, Long> {}
