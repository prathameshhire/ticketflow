import { DatePipe, NgClass, NgFor, NgIf } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { PageResponse, Ticket, TicketFilters, TicketPriority, TicketStatus } from '../../core/models';

@Component({
  selector: 'tf-ticket-list-page',
  standalone: true,
  imports: [DatePipe, NgClass, NgFor, NgIf, ReactiveFormsModule, RouterLink],
  templateUrl: './ticket-list-page.component.html'
})
export class TicketListPageComponent implements OnInit {
  page: PageResponse<Ticket> | null = null;
  isLoading = false;
  errorMessage = '';
  readonly statuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
  readonly priorities: TicketPriority[] = ['URGENT', 'HIGH', 'MEDIUM', 'LOW'];
  readonly pageSizes = [5, 10, 20, 50];

  readonly filtersForm = this.fb.nonNullable.group({
    q: [''],
    status: [''],
    priority: [''],
    assignedAgentId: [''],
    customerId: [''],
    size: [10],
    sort: ['createdAt']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly api: ApiService,
    private readonly auth: AuthService
  ) {
  }

  ngOnInit(): void {
    this.loadTickets(0);
  }

  applyFilters(): void {
    this.loadTickets(0);
  }

  clearFilters(): void {
    this.filtersForm.reset({
      q: '',
      status: '',
      priority: '',
      assignedAgentId: '',
      customerId: '',
      size: 10,
      sort: 'createdAt'
    });
    this.loadTickets(0);
  }

  nextPage(): void {
    if (this.page && !this.page.last) {
      this.loadTickets(this.page.page + 1);
    }
  }

  previousPage(): void {
    if (this.page && !this.page.first) {
      this.loadTickets(this.page.page - 1);
    }
  }

  private loadTickets(page: number): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.api.listTickets(this.toFilters(page)).subscribe({
      next: (response) => {
        this.page = response;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load tickets.';
        this.isLoading = false;
      }
    });
  }

  private toFilters(page: number): TicketFilters {
    const value = this.filtersForm.getRawValue();
    return {
      page,
      size: Number(value.size),
      q: value.q,
      status: value.status as TicketStatus | '',
      priority: value.priority as TicketPriority | '',
      assignedAgentId: value.assignedAgentId ? Number(value.assignedAgentId) : null,
      customerId: value.customerId ? Number(value.customerId) : null,
      sort: value.sort as 'createdAt' | 'priority'
    };
  }

  canCreateTicket(): boolean {
    return this.auth.currentUser?.role === 'CUSTOMER';
  }
}
