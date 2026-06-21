package com.acidtango.productsorter.adapter.rest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class VersioningConfig implements WebMvcConfigurer {

  @Override
  public void configurePathMatch(final PathMatchConfigurer configurer) {
    configurer.addPathPrefix("/api/v1", c -> true);
  }
}
