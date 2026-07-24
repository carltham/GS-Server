package com.gsserver.ui.terminal;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Authorises the terminal WebSocket handshake by redeeming a one-time ticket.
 *
 * <p>The client first calls the authenticated {@code POST /api/v1/terminal/ticket} endpoint and then
 * opens the socket with {@code ?ticket=<value>}. The ticket is redeemed exactly once here; a missing,
 * expired, or already-used ticket rejects the handshake before it reaches the terminal handler.
 */
public class TerminalAuthHandshakeInterceptor implements HandshakeInterceptor {
  private static final Logger logger =
      LoggerFactory.getLogger(TerminalAuthHandshakeInterceptor.class);
  private static final String TICKET_PARAM = "ticket";
  static final String AUTH_USER_ATTRIBUTE = "gs.terminal.user";

  private final TerminalTicketService ticketService;

  public TerminalAuthHandshakeInterceptor(TerminalTicketService ticketService) {
    this.ticketService = ticketService;
  }

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {

    String ticket = extractTicket(request);
    Optional<String> username = ticketService.redeem(ticket);
    if (username.isEmpty()) {
      logger.info("Rejected terminal handshake: invalid or expired ticket");
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }

    attributes.put(AUTH_USER_ATTRIBUTE, username.get());
    logger.debug("Terminal handshake authorised for user {}", username.get());
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    // no-op
  }

  private String extractTicket(ServerHttpRequest request) {
    String query = request.getURI().getQuery();
    if (query == null) {
      return null;
    }
    for (String pair : query.split("&")) {
      int eq = pair.indexOf('=');
      if (eq > 0 && TICKET_PARAM.equals(pair.substring(0, eq))) {
        return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
      }
    }
    return null;
  }
}
