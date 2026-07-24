package com.gsserver.ui.admin;

import com.gsserver.ui.security.JsonUserStore;
import com.gsserver.ui.security.ManagedUser;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * User CRUD with the admin/superadmin authorization model:
 *
 * <ul>
 *   <li><b>Superadmin</b> ({@code GROUP_SUPERUSER}) manages every account and may grant any role.
 *   <li><b>Admin</b> ({@code GROUP_HARDENING_ADMINS}) manages only non-elevated accounts and may not
 *       grant admin/superadmin roles.
 * </ul>
 *
 * Also guards against self-lockout (deleting/disabling yourself) and removing the last superadmin.
 */
@Service
public class UserManagementService {

  static final String ADMIN = "GROUP_HARDENING_ADMINS";
  static final String SUPERADMIN = "GROUP_SUPERUSER";
  private static final Set<String> ELEVATED = Set.of(ADMIN, SUPERADMIN);
  private static final Set<String> KNOWN_AUTHORITIES =
      Set.of("GROUP_HARDENING_OPERATORS", ADMIN, "GROUP_AUDIT_READERS", SUPERADMIN);
  private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9._-]{2,32}$");

  private final JsonUserStore userStore;
  private final PasswordEncoder passwordEncoder;

  public UserManagementService(JsonUserStore userStore, PasswordEncoder passwordEncoder) {
    this.userStore = userStore;
    this.passwordEncoder = passwordEncoder;
  }

  public List<UserSummary> list() {
    return userStore.findAll().stream()
        .map(u -> new UserSummary(u.username(), u.authorities(), u.enabled()))
        .toList();
  }

  public UserSummary create(Authentication caller, CreateUserRequest request) {
    String username = requireValidUsername(request.username());
    if (userStore.exists(username)) {
      throw status(HttpStatus.CONFLICT, "User already exists: " + username);
    }
    if (isBlank(request.password())) {
      throw status(HttpStatus.BAD_REQUEST, "Password is required.");
    }
    List<String> authorities = normalizeAuthorities(request.authorities());
    requireCanAssign(caller, authorities);

    boolean enabled = request.enabled() == null || request.enabled();
    ManagedUser user =
        new ManagedUser(username, passwordEncoder.encode(request.password()), authorities, enabled);
    userStore.save(user);
    return new UserSummary(user.username(), user.authorities(), user.enabled());
  }

  public UserSummary update(Authentication caller, String username, UpdateUserRequest request) {
    ManagedUser existing = require(username);
    requireCanManage(caller, existing);

    List<String> authorities = normalizeAuthorities(request.authorities());
    requireCanAssign(caller, authorities);

    boolean enabled = request.enabled() == null || request.enabled();
    if (!enabled && isSelf(caller, username)) {
      throw status(HttpStatus.BAD_REQUEST, "You cannot disable your own account.");
    }

    ManagedUser updated = existing.withAuthorities(authorities).withEnabled(enabled);
    ensureNotRemovingLastSuperadmin(existing, updated);
    userStore.save(updated);
    return new UserSummary(updated.username(), updated.authorities(), updated.enabled());
  }

  public void resetPassword(Authentication caller, String username, PasswordResetRequest request) {
    ManagedUser existing = require(username);
    requireCanManage(caller, existing);
    if (isBlank(request.password())) {
      throw status(HttpStatus.BAD_REQUEST, "Password is required.");
    }
    userStore.save(existing.withPasswordHash(passwordEncoder.encode(request.password())));
  }

  public void delete(Authentication caller, String username) {
    ManagedUser existing = require(username);
    requireCanManage(caller, existing);
    if (isSelf(caller, username)) {
      throw status(HttpStatus.BAD_REQUEST, "You cannot delete your own account.");
    }
    ensureNotRemovingLastSuperadmin(existing, null);
    userStore.delete(username);
  }

  // ---- authorization helpers ----

  private void requireCanManage(Authentication caller, ManagedUser target) {
    if (isSuperadmin(caller)) {
      return;
    }
    // Admin: may only manage non-elevated accounts.
    if (target.authorities().stream().anyMatch(ELEVATED::contains)) {
      throw status(HttpStatus.FORBIDDEN, "Only a superadmin can manage admin/superadmin accounts.");
    }
  }

  private void requireCanAssign(Authentication caller, List<String> authorities) {
    if (isSuperadmin(caller)) {
      return;
    }
    if (authorities.stream().anyMatch(ELEVATED::contains)) {
      throw status(HttpStatus.FORBIDDEN, "Only a superadmin can grant admin/superadmin roles.");
    }
  }

  private void ensureNotRemovingLastSuperadmin(ManagedUser before, ManagedUser afterOrNull) {
    boolean wasEnabledSuperadmin = before.enabled() && before.authorities().contains(SUPERADMIN);
    boolean stillEnabledSuperadmin =
        afterOrNull != null
            && afterOrNull.enabled()
            && afterOrNull.authorities().contains(SUPERADMIN);
    if (wasEnabledSuperadmin && !stillEnabledSuperadmin
        && userStore.countEnabledWithAuthority(SUPERADMIN) <= 1) {
      throw status(HttpStatus.BAD_REQUEST, "At least one enabled superadmin must remain.");
    }
  }

  private boolean isSuperadmin(Authentication caller) {
    return caller.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(SUPERADMIN::equals);
  }

  private boolean isSelf(Authentication caller, String username) {
    return caller.getName().equals(username);
  }

  // ---- validation helpers ----

  private ManagedUser require(String username) {
    return userStore
        .findByUsername(username)
        .orElseThrow(() -> status(HttpStatus.NOT_FOUND, "Unknown user: " + username));
  }

  private String requireValidUsername(String username) {
    if (username == null || !USERNAME.matcher(username).matches()) {
      throw status(HttpStatus.BAD_REQUEST, "Invalid username.");
    }
    return username;
  }

  private List<String> normalizeAuthorities(List<String> authorities) {
    List<String> normalized = authorities == null ? List.of() : authorities.stream().distinct().toList();
    for (String authority : normalized) {
      if (!KNOWN_AUTHORITIES.contains(authority)) {
        throw status(HttpStatus.BAD_REQUEST, "Unknown authority: " + authority);
      }
    }
    return normalized;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private ResponseStatusException status(HttpStatus status, String message) {
    return new ResponseStatusException(status, message);
  }
}
