package com.gsserver.ui.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityUsersProperties.class)
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
      .authorizeHttpRequests(auth -> auth
          // The terminal WebSocket authorizes itself via a one-time ticket at the handshake.
          .requestMatchers("/api/v1/terminal/ws").permitAll()
          // Public login-screen configuration (needed before authenticating).
          .requestMatchers("/api/v1/auth/config").permitAll()
          // Protect the data API; the SPA authenticates these calls after login.
          .requestMatchers("/api/**").authenticated()
          // The Angular shell (index.html, JS/CSS bundles, deep-link routes) must load
          // unauthenticated so the login screen can appear when served by the backend.
          .anyRequest().permitAll())
        .httpBasic(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  JsonUserStore jsonUserStore(
      SecurityUsersProperties securityUsersProperties, PasswordEncoder passwordEncoder) {
    return new JsonUserStore(securityUsersProperties, passwordEncoder);
  }

  @Bean
  UserDetailsService userDetailsService(JsonUserStore jsonUserStore) {
    return new JsonUserDetailsService(jsonUserStore);
  }

  @Bean
  AuthenticationProvider authenticationProvider(
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder,
      SecurityUsersProperties securityUsersProperties) {
    LocalhostThorAuthenticationProvider provider = new LocalhostThorAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    provider.setThorLoginEnabled(securityUsersProperties.isThorLoginEnabled());
    return provider;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
