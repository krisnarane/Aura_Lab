package br.com.auralab.vendas.repository;

import br.com.auralab.vendas.domain.Cupom;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório de cupons: créditos de troca do cliente e cupom promocional por código. */
public interface CupomRepository extends JpaRepository<Cupom, Long> {

  List<Cupom> findByClienteIdAndTipoOrderByIdDesc(Long clienteId, String tipo);

  Optional<Cupom> findByCodigoAndTipo(String codigo, String tipo);
}
