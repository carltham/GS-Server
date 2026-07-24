import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../core/auth.service';
import {
  ManagedUserSummary,
  UserAdminService
} from './user-admin.service';

interface RoleOption {
  authority: string;
  label: string;
  elevated: boolean;
}

interface EditableUser {
  username: string;
  authorities: string[];
  enabled: boolean;
  busy: boolean;
}

@Component({
  selector: 'gs-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class UsersComponent implements OnInit {
  readonly roles: RoleOption[] = [
    { authority: 'GROUP_HARDENING_OPERATORS', label: 'Operator', elevated: false },
    { authority: 'GROUP_AUDIT_READERS', label: 'Auditor', elevated: false },
    { authority: 'GROUP_HARDENING_ADMINS', label: 'Admin', elevated: true },
    { authority: 'GROUP_SUPERUSER', label: 'Superadmin', elevated: true }
  ];

  users: EditableUser[] = [];
  loading = false;
  error = '';
  info = '';

  newUsername = '';
  newPassword = '';
  newAuthorities: string[] = [];
  newEnabled = true;

  constructor(
    private readonly api: UserAdminService,
    private readonly auth: AuthService
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  get isSuperadmin(): boolean {
    return this.auth.authorities.includes('GROUP_SUPERUSER');
  }

  /** Admins may not manage accounts that hold an elevated (admin/superadmin) role. */
  canManage(user: EditableUser): boolean {
    if (this.isSuperadmin) {
      return true;
    }
    return !user.authorities.some((a) => this.isElevated(a));
  }

  canAssign(role: RoleOption): boolean {
    return this.isSuperadmin || !role.elevated;
  }

  isElevated(authority: string): boolean {
    return this.roles.some((r) => r.authority === authority && r.elevated);
  }

  reload(): void {
    this.loading = true;
    this.error = '';
    this.api.list().subscribe({
      next: (users) => {
        this.users = users.map((u) => this.toEditable(u));
        this.loading = false;
      },
      error: (err) => {
        this.error = this.message(err, 'Failed to load users.');
        this.loading = false;
      }
    });
  }

  hasRole(user: EditableUser, authority: string): boolean {
    return user.authorities.includes(authority);
  }

  toggleRole(user: EditableUser, authority: string): void {
    if (user.authorities.includes(authority)) {
      user.authorities = user.authorities.filter((a) => a !== authority);
    } else {
      user.authorities = [...user.authorities, authority];
    }
  }

  save(user: EditableUser): void {
    user.busy = true;
    this.clearMessages();
    this.api.update(user.username, { authorities: user.authorities, enabled: user.enabled }).subscribe({
      next: () => {
        user.busy = false;
        this.info = `Saved ${user.username}.`;
      },
      error: (err) => {
        user.busy = false;
        this.error = this.message(err, 'Failed to save user.');
        this.reload();
      }
    });
  }

  resetPassword(user: EditableUser): void {
    const password = window.prompt(`New password for ${user.username}:`);
    if (!password) {
      return;
    }
    user.busy = true;
    this.clearMessages();
    this.api.resetPassword(user.username, password).subscribe({
      next: () => {
        user.busy = false;
        this.info = `Password reset for ${user.username}.`;
      },
      error: (err) => {
        user.busy = false;
        this.error = this.message(err, 'Failed to reset password.');
      }
    });
  }

  remove(user: EditableUser): void {
    if (!window.confirm(`Delete user ${user.username}? This cannot be undone.`)) {
      return;
    }
    user.busy = true;
    this.clearMessages();
    this.api.delete(user.username).subscribe({
      next: () => {
        this.users = this.users.filter((u) => u.username !== user.username);
        this.info = `Deleted ${user.username}.`;
      },
      error: (err) => {
        user.busy = false;
        this.error = this.message(err, 'Failed to delete user.');
      }
    });
  }

  toggleNewRole(authority: string): void {
    if (this.newAuthorities.includes(authority)) {
      this.newAuthorities = this.newAuthorities.filter((a) => a !== authority);
    } else {
      this.newAuthorities = [...this.newAuthorities, authority];
    }
  }

  createUser(): void {
    this.clearMessages();
    if (!this.newUsername.trim() || !this.newPassword) {
      this.error = 'Username and password are required.';
      return;
    }
    this.api
      .create({
        username: this.newUsername.trim(),
        password: this.newPassword,
        authorities: this.newAuthorities,
        enabled: this.newEnabled
      })
      .subscribe({
        next: () => {
          this.info = `Created ${this.newUsername.trim()}.`;
          this.newUsername = '';
          this.newPassword = '';
          this.newAuthorities = [];
          this.newEnabled = true;
          this.reload();
        },
        error: (err) => {
          this.error = this.message(err, 'Failed to create user.');
        }
      });
  }

  private toEditable(user: ManagedUserSummary): EditableUser {
    return {
      username: user.username,
      authorities: [...user.authorities],
      enabled: user.enabled,
      busy: false
    };
  }

  private clearMessages(): void {
    this.error = '';
    this.info = '';
  }

  private message(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse) {
      const msg = (err.error as { message?: unknown } | null)?.message;
      if (typeof msg === 'string' && msg.trim().length > 0) {
        return msg;
      }
    }
    return fallback;
  }
}
