import { DecimalPipe, NgFor, NgIf } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { AgentWorkload, DashboardSummary, TicketPriority, TicketStatus } from '../../core/models';

interface DashboardMetric {
  label: string;
  value: number | string;
  caption: string;
  spark: number[];
  highlight?: boolean;
}

@Component({
  selector: 'tf-dashboard-page',
  standalone: true,
  imports: [DecimalPipe, NgFor, NgIf, RouterLink],
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

  metricCards(summary: DashboardSummary): DashboardMetric[] {
    return [
      {
        label: 'Total tickets',
        value: summary.totalTickets,
        caption: `${summary.openCount + summary.inProgressCount} active right now`,
        spark: [42, 52, 50, 64, 68, 72, 58, 46, 44, 36, 39, 43],
        highlight: true
      },
      {
        label: 'Open queue',
        value: summary.openCount,
        caption: 'Awaiting first response',
        spark: [26, 28, 36, 31, 44, 58, 48, 42, 37, 33, 30, 35]
      },
      {
        label: 'In progress',
        value: summary.inProgressCount,
        caption: 'Owned by agents',
        spark: [32, 38, 34, 42, 55, 61, 50, 47, 43, 52, 49, 45]
      },
      {
        label: 'Overdue SLA',
        value: summary.overdueSlaCount,
        caption: 'Needs attention',
        spark: [18, 24, 22, 30, 36, 41, 39, 45, 52, 57, 62, 66]
      }
    ];
  }

  statusLabel(status: TicketStatus): string {
    return status.replace('_', ' ');
  }

  priorityShare(summary: DashboardSummary, priority: TicketPriority): number {
    return this.share(summary.ticketsByPriority[priority] || 0, summary.totalTickets);
  }

  statusShare(summary: DashboardSummary, status: TicketStatus): number {
    return this.share(summary.ticketsByStatus[status] || 0, summary.totalTickets);
  }

  agentLoadPercent(agent: AgentWorkload, summary: DashboardSummary): number {
    const maxAssigned = Math.max(...summary.agentWorkload.map((item) => item.totalAssigned), 1);
    return Math.round((agent.totalAssigned / maxAssigned) * 100);
  }

  priorityDonut(summary: DashboardSummary): string {
    const urgent = this.priorityShare(summary, 'URGENT');
    const high = urgent + this.priorityShare(summary, 'HIGH');
    const medium = high + this.priorityShare(summary, 'MEDIUM');

    return `conic-gradient(#ff7a5c 0 ${urgent}%, #f4c542 ${urgent}% ${high}%, #5aa9ff ${high}% ${medium}%, #2fd3a6 ${medium}% 100%)`;
  }

  private share(value: number, total: number): number {
    if (total <= 0) {
      return 0;
    }

    return Math.max(4, Math.round((value / total) * 100));
  }
}
