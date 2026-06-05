import { DatePipe, NgClass, NgFor, NgIf } from '@angular/common';
import { Component, OnInit } from '@angular/core';

import { ApiService } from '../../core/api.service';
import { Alert } from '../../core/models';

@Component({
  selector: 'tf-alerts-page',
  standalone: true,
  imports: [DatePipe, NgClass, NgFor, NgIf],
  templateUrl: './alerts-page.component.html'
})
export class AlertsPageComponent implements OnInit {
  alerts: Alert[] = [];
  errorMessage = '';
  successMessage = '';
  isLoading = false;
  isMarkingAll = false;
  private readonly pendingReadIds = new Set<number>();

  constructor(private readonly api: ApiService) {
  }

  ngOnInit(): void {
    this.loadAlerts();
  }

  markRead(alert: Alert): void {
    if (alert.readFlag || this.pendingReadIds.has(alert.id)) {
      return;
    }

    this.clearMessages();
    this.pendingReadIds.add(alert.id);
    this.api.markAlertRead(alert.id).subscribe({
      next: (updated) => {
        this.alerts = this.alerts.map((item) => item.id === updated.id ? updated : item);
        this.pendingReadIds.delete(alert.id);
      },
      error: () => {
        this.errorMessage = 'Unable to mark alert as read.';
        this.pendingReadIds.delete(alert.id);
      }
    });
  }

  markAllRead(): void {
    this.clearMessages();
    this.isMarkingAll = true;
    this.api.markAllAlertsRead().subscribe({
      next: (response) => {
        this.alerts = this.alerts.map((alert) => ({ ...alert, readFlag: true }));
        this.successMessage = `${response.updatedCount} alerts marked read.`;
        this.isMarkingAll = false;
      },
      error: () => {
        this.errorMessage = 'Unable to mark alerts as read.';
        this.isMarkingAll = false;
      }
    });
  }

  hasUnreadAlerts(): boolean {
    return this.alerts.some((alert) => !alert.readFlag);
  }

  isMarkingRead(id: number): boolean {
    return this.pendingReadIds.has(id);
  }

  private loadAlerts(): void {
    this.isLoading = true;
    this.api.listAlerts().subscribe({
      next: (alerts) => {
        this.alerts = alerts;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load alerts.';
        this.isLoading = false;
      }
    });
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
