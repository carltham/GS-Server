package com.gsserver.ui.terminal;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Issues and redeems short-lived, single-use tickets that authorise a terminal WebSocket handshake.
 *
 * <p>A browser cannot attach an {@code Authorization} header to a WebSocket handshake. Rather than
 * placing a long-lived credential in the WS URL (where it would leak into logs and browser history),
 * an already-authenticated HTTP request mints a ticket here; the handshake then redeems it exactly
 * once, within a few seconds. A ticket is worthless after it is used or after it expires.
 */
@Service
public class TerminalTicketService {
  private static final Duration TICKET_TTL = Duration.ofSeconds(30);
  private static final int TICKET_BYTES = 32; // 256 bits of entropy

  private final SecureRandom random = new SecureRandom();
  private final ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<>();

  /**
   * Mint a single-use ticket bound to the given user.
   *
   * @param username the authenticated user the ticket is issued to
   * @return an opaque ticket string to be redeemed at the WebSocket handshake
   */
  public String issue(String username) {
    purgeExpired();
    byte[] buffer = new byte[TICKET_BYTES];
    random.nextBytes(buffer);
    String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    tickets.put(ticket, new Ticket(username, Instant.now().plus(TICKET_TTL)));
    return ticket;
  }

  /**
   * Atomically consume a ticket. A ticket can be redeemed at most once and only before it expires.
   *
   * @param ticket the ticket presented at the handshake
   * @return the username the ticket was issued to, or empty if missing/expired/already used
   */
  public Optional<String> redeem(String ticket) {
    if (ticket == null || ticket.isBlank()) {
      return Optional.empty();
    }
    Ticket consumed = tickets.remove(ticket); // single-use: removed regardless of validity
    if (consumed == null || consumed.expiresAt().isBefore(Instant.now())) {
      return Optional.empty();
    }
    return Optional.of(consumed.username());
  }

  private void purgeExpired() {
    Instant now = Instant.now();
    tickets.values().removeIf(ticket -> ticket.expiresAt().isBefore(now));
  }

  private record Ticket(String username, Instant expiresAt) {}
}
