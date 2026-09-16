package br.com.auralab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
