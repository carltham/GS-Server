package com.gsserver.ui.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration to allow cross-origin requests from development frontend.
 */
@Configuration
public class CorsConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    // Allow requests from localhost:4200 (Angular dev server)
    configuration.addAllowedOrigin("http://localhost:4200");
    configuration.addAllowedOrigin("http://127.0.0.1:4200");

    // Allow the production origins (served behind Cloudflare/nginx). The WebSocket handshake and
    // any cross-scheme request (browser https -> origin http) pass through this CORS check.
    configuration.addAllowedOrigin("https://serveradmin.gustafsweb.services");
    configuration.addAllowedOrigin("https://gustafsweb.services");
    configuration.addAllowedOrigin("https://www.gustafsweb.services");
    
    // Allow common HTTP methods
    configuration.addAllowedMethod("GET");
    configuration.addAllowedMethod("POST");
    configuration.addAllowedMethod("PUT");
    configuration.addAllowedMethod("DELETE");
    configuration.addAllowedMethod("OPTIONS");
    configuration.addAllowedMethod("PATCH");
    
    // Allow common headers
    configuration.addAllowedHeader("*");
    
    // Allow credentials (cookies, auth headers)
    configuration.setAllowCredentials(true);
    
    // Cache preflight responses for 1 hour
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
