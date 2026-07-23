import { Component } from '@angular/core';

import { HomeApiService } from './home-api.service';

@Component({
  selector: 'gs-home',
  templateUrl: './home.component.html'
})
export class HomeComponent {
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
      error: () => {
        this.statusMessage = 'Hardening request failed.';
        this.isSubmitting = false;
      }
    });
  }
}
