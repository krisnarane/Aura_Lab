package br.com.auralab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Roteamento do frontend: encaminha as rotas do SPA para index.html. Não há autenticação
 * nesta etapa (decisão do projeto); os módulos integrados são /admin/clientes,
 * /admin/produtos e /perfil/**.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    for (String rota :
        new String[] {
          "/admin/**",
          "/perfil/**",
          "/checkout/**",
          "/produto/**",
          "/catalogo",
          "/cadastro",
          "/sacola",
          "/consultora"
        }) registry.addViewController(rota).setViewName("forward:/index.html");
  }
}
