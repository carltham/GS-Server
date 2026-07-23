package com.gsserver.ui.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityUsersProperties.class)
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
      .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .httpBasic(Customizer.withDefaults());

    return http.build();
  }

  @Bean
    AuthenticationProvider authenticationProvider(
      UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    LocalhostThorAuthenticationProvider provider = new LocalhostThorAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
    }

    @Bean
  UserDetailsService userDetailsService(
      PasswordEncoder passwordEncoder, SecurityUsersProperties securityUsersProperties) {
    if (securityUsersProperties.getUsers().isEmpty()) {
      throw new IllegalStateException("No users configured under gsserver.security.users");
    }

    return new InMemoryUserDetailsManager(
        securityUsersProperties.getUsers().stream()
            .map(
                user ->
                    User.withUsername(user.getUsername())
                        .password(passwordEncoder.encode(user.getPassword()))
                        .authorities(user.getAuthorities().toArray(new String[0]))
                        .build())
            .collect(Collectors.toList()));
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
