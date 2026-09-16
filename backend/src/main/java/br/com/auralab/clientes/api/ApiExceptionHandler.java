package br.com.auralab.clientes.api;

import br.com.auralab.clientes.api.dto.ErroView;
import br.com.auralab.clientes.service.RegraNegocioException;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(RegraNegocioException.class)
  ResponseEntity<ErroView> regra(RegraNegocioException e) {
    return ResponseEntity.status(e.getStatus())
        .body(new ErroView(e.getCodigo(), e.getMessage(), e.getCampos()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErroView> validacao(MethodArgumentNotValidException e) {
    Map<String, String> campos = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(x -> campos.putIfAbsent(x.getField(), x.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(new ErroView("DADOS_INVALIDOS", "Revise os campos informados.", campos));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ErroView> integridade() {
    return ResponseEntity.status(409)
        .body(
            new ErroView(
                "CONFLITO_DADOS",
                "Os dados informados entram em conflito com um cadastro existente.",
                Map.of()));
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ErroView> formato() {
    return ResponseEntity.badRequest()
        .body(
            new ErroView(
                "FORMATO_INVALIDO", "Revise os tipos e formatos dos dados enviados.", Map.of()));
  }
}
