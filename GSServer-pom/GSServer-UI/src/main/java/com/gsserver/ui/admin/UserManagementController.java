package com.gsserver.ui.admin;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin/superadmin user-management API. Access is limited to admins and superadmins; the finer
 * admin-vs-superadmin rules are enforced in {@link UserManagementService}.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAnyAuthority('GROUP_HARDENING_ADMINS','GROUP_SUPERUSER')")
public class UserManagementController {

  private final UserManagementService service;

  public UserManagementController(UserManagementService service) {
    this.service = service;
  }

  @GetMapping
  public List<UserSummary> list() {
    return service.list();
  }

  @PostMapping
  public ResponseEntity<UserSummary> create(
      Authentication caller, @RequestBody CreateUserRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(caller, request));
  }

  @PutMapping("/{username}")
  public UserSummary update(
      Authentication caller,
      @PathVariable String username,
      @RequestBody UpdateUserRequest request) {
    return service.update(caller, username, request);
  }

  @PutMapping("/{username}/password")
  public ResponseEntity<Void> resetPassword(
      Authentication caller,
      @PathVariable String username,
      @RequestBody PasswordResetRequest request) {
    service.resetPassword(caller, username, request);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{username}")
  public ResponseEntity<Void> delete(Authentication caller, @PathVariable String username) {
    service.delete(caller, username);
    return ResponseEntity.noContent().build();
  }
}
