package br.com.auralab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Roteamento do frontend: encaminha as rotas do SPA para index.html. Não há autenticação nesta
 * etapa (decisão do projeto); os módulos integrados incluem administração, perfil e a jornada
 * principal de vendas (catálogo, sacola e checkout).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    for (String rota :
        new String[] {
          "/admin/**",
          "/perfil/**",
          "/loja",
          "/loja/**",
          "/checkout/**",
          "/produto/**",
          "/catalogo",
          "/cadastro",
          "/sacola",
          "/consultora"
        }) registry.addViewController(rota).setViewName("forward:/index.html");
  }
}
