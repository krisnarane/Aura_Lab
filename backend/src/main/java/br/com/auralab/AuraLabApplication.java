package br.com.auralab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do backend Aura Lab (Spring Boot + PostgreSQL/Flyway). Módulos atuais:
 * clientes (RF0021 a RF0028) e produtos (RF0011, RF0012, RF0015, RF0016).
 */
@SpringBootApplication
public class AuraLabApplication {
  public static void main(String[] args) {
    SpringApplication.run(AuraLabApplication.class, args);
  }
}
