package com.gsserver.ui.terminal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final TerminalSessionManager sessionManager;
  private final TerminalTicketService ticketService;

  public WebSocketConfig(
      TerminalSessionManager sessionManager, TerminalTicketService ticketService) {
    this.sessionManager = sessionManager;
    this.ticketService = ticketService;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(terminalWebSocketHandler(), "/api/v1/terminal/ws")
        .setAllowedOriginPatterns(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://serveradmin.gustafsweb.services",
            "https://gustafsweb.services",
            "https://www.gustafsweb.services")
        .addInterceptors(new TerminalAuthHandshakeInterceptor(ticketService));
  }

  @Bean
  public TerminalWebSocketHandler terminalWebSocketHandler() {
    return new TerminalWebSocketHandler(sessionManager);
  }
}
