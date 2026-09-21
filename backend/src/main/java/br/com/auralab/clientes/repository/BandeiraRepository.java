package br.com.auralab.clientes.repository;

import br.com.auralab.clientes.domain.Bandeira;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório da tabela de domínio de bandeiras (RN0025); usado pela consulta de bandeiras no CartaoService. */
public interface BandeiraRepository extends JpaRepository<Bandeira, Long> {}
