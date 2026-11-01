package br.com.auralab.vendas;

import static br.com.auralab.vendas.domain.MotivoReprovacao.*;
import static org.assertj.core.api.Assertions.*;

import br.com.auralab.vendas.domain.MotivoReprovacao;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** RF0039/RN0037–RN0040: a precedência dos motivos de reprovação é explícita, não acidental. */
class MotivoReprovacaoTest {

  @Test
  void RF0039_semViolacaoNaoHaMotivoEOPagamentoEAprovado() {
    assertThat(MotivoReprovacao.prioritario(Set.of())).isEmpty();
  }

  @Test
  void RF0039_violacaoUnicaEOMotivoRegistrado() {
    for (MotivoReprovacao motivo : values())
      assertThat(MotivoReprovacao.prioritario(Set.of(motivo))).contains(motivo);
  }

  @Test
  void RN0040_reservaExpiradaPrevaleceSobreOResultadoDaOperadora() {
    assertThat(MotivoReprovacao.prioritario(EnumSet.of(OPERADORA_RECUSOU, RESERVA_EXPIRADA)))
        .contains(RESERVA_EXPIRADA);
    assertThat(MotivoReprovacao.prioritario(EnumSet.of(RESERVA_EXPIRADA, OPERADORA_RECUSOU)))
        .contains(RESERVA_EXPIRADA);
  }

  @Test
  void RN0037_RN0038_causaEstruturalPrevaleceSobreCupomEOCupomSobreOCartao() {
    assertThat(MotivoReprovacao.prioritario(EnumSet.of(CUPOM_INVALIDO, RESERVA_INVALIDA)))
        .contains(RESERVA_INVALIDA);
    assertThat(MotivoReprovacao.prioritario(EnumSet.of(OPERADORA_RECUSOU, CUPOM_INVALIDO)))
        .contains(CUPOM_INVALIDO);
  }

  @Test
  void RN0039_falhaTecnicaDaOperadoraPrevaleceSobreRecusaSimples() {
    assertThat(
            MotivoReprovacao.prioritario(
                EnumSet.of(OPERADORA_RECUSOU, OPERADORA_INDISPONIVEL, OPERADORA_RECUSOU)))
        .contains(OPERADORA_INDISPONIVEL);
  }

  @Test
  void RF0039_resultadoNaoDependeDaOrdemDeDeteccao() {
    var todas = List.copyOf(EnumSet.allOf(MotivoReprovacao.class));
    assertThat(MotivoReprovacao.prioritario(todas)).contains(RESERVA_EXPIRADA);
    assertThat(MotivoReprovacao.prioritario(todas.reversed())).contains(RESERVA_EXPIRADA);
  }

  @Test
  void RF0039_ordemDeclaradaEAPrecedenciaDocumentada() {
    assertThat(values())
        .containsExactly(
            RESERVA_EXPIRADA,
            RESERVA_INVALIDA,
            CUPOM_INVALIDO,
            OPERADORA_INDISPONIVEL,
            OPERADORA_RECUSOU);
  }
}
