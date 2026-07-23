import { Component } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { HomeApiService } from './home-api.service';

@Component({
  selector: 'gs-hardening',
  templateUrl: './hardening.component.html'
})
export class HardeningComponent {
  isSubmitting = false;
  statusMessage = 'Ready to run hardening.';

  constructor(private readonly homeApiService: HomeApiService) {}

  runHardening(): void {
    if (this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;
    this.statusMessage = 'Submitting hardening request...';

    this.homeApiService.triggerHardening().subscribe({
      next: (response) => {
        this.statusMessage = response.message;
        this.isSubmitting = false;
      },
      error: (error: unknown) => {
        this.statusMessage = this.resolveErrorMessage(error);
        this.isSubmitting = false;
      }
    });
  }

  private resolveErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const errorMessage = (error.error as { message?: unknown } | null)?.message;
      if (typeof errorMessage === 'string' && errorMessage.trim().length > 0) {
        return errorMessage;
      }
    }

    return 'Hardening request failed.';
  }
}
