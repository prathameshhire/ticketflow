import { DecimalPipe, NgFor, NgIf } from '@angular/common';
import { Component, OnInit } from '@angular/core';

import { ApiService } from '../../core/api.service';
import { DashboardSummary, TicketPriority, TicketStatus } from '../../core/models';

@Component({
  selector: 'tf-dashboard-page',
  standalone: true,
  imports: [DecimalPipe, NgFor, NgIf],
  templateUrl: './dashboard-page.component.html'
})
export class DashboardPageComponent implements OnInit {
  summary: DashboardSummary | null = null;
  isLoading = false;
  errorMessage = '';
  readonly priorities: TicketPriority[] = ['URGENT', 'HIGH', 'MEDIUM', 'LOW'];
  readonly statuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

  constructor(private readonly api: ApiService) {
  }

  ngOnInit(): void {
    this.isLoading = true;
    this.api.dashboardSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load dashboard summary.';
        this.isLoading = false;
      }
    });
  }
}
