package com.gsserver.ui.terminal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mints short-lived, single-use tickets for opening the interactive terminal WebSocket.
 *
 * <p>This endpoint runs under the normal HTTP security filter chain, so it requires the caller's
 * existing credentials. The issued ticket is the only thing that travels in the WebSocket URL.
 */
@RestController
@RequestMapping("/api/v1/terminal")
public class TerminalTicketController {

  private final TerminalTicketService ticketService;

  public TerminalTicketController(TerminalTicketService ticketService) {
    this.ticketService = ticketService;
  }

  @PostMapping("/ticket")
  @PreAuthorize("hasAnyAuthority('GROUP_HARDENING_OPERATORS','GROUP_HARDENING_ADMINS','GROUP_SUPERUSER')")
  public ResponseEntity<TicketResponse> issue(Authentication authentication) {
    String ticket = ticketService.issue(authentication.getName());
    return ResponseEntity.ok(new TicketResponse(ticket));
  }

  /** Response body carrying the one-time ticket. */
  public record TicketResponse(String ticket) {}
}
