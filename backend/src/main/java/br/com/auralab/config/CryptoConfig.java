package br.com.auralab.config;

import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Encoder de senha em BCrypt com custo 10 (RNF0033: senha criptografada, texto puro nunca
 * persistido).
 */
@Configuration
public class CryptoConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }
}
