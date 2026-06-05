import { NgFor, NgIf } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { TicketPriority } from '../../core/models';

@Component({
  selector: 'tf-ticket-create-page',
  standalone: true,
  imports: [NgFor, NgIf, ReactiveFormsModule, RouterLink],
  templateUrl: './ticket-create-page.component.html'
})
export class TicketCreatePageComponent {
  errorMessage = '';
  isSubmitting = false;
  readonly priorities: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(160)]],
    description: ['', [Validators.required, Validators.maxLength(5000)]],
    priority: ['MEDIUM' as TicketPriority, [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly api: ApiService,
    private readonly auth: AuthService,
    private readonly router: Router
  ) {
  }

  submit(): void {
    if (!this.canCreateTicket()) {
      this.errorMessage = 'Only customer accounts can create tickets.';
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.api.createTicket(this.form.getRawValue()).subscribe({
      next: (ticket) => void this.router.navigate(['/tickets', ticket.id]),
      error: (error) => {
        this.errorMessage = error.error?.message || 'Unable to create ticket.';
        this.isSubmitting = false;
      }
    });
  }

  canCreateTicket(): boolean {
    return this.auth.currentUser?.role === 'CUSTOMER';
  }
}
