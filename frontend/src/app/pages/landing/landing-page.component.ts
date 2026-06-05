import { Component } from '@angular/core';
import { NgFor } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'tf-landing-page',
  standalone: true,
  imports: [NgFor, RouterLink],
  templateUrl: './landing-page.component.html'
})
export class LandingPageComponent {
  readonly heroTickets = [
    {
      company: 'AtlasPay',
      title: 'Checkout API latency crossing SLA',
      priority: 'URGENT',
      status: 'IN PROGRESS',
      due: '3h 42m'
    },
    {
      company: 'Northstar Health',
      title: 'Customer portal session failures',
      priority: 'HIGH',
      status: 'OPEN',
      due: '8h 10m'
    },
    {
      company: 'BrightDesk',
      title: 'Billing export missing newest invoices',
      priority: 'MEDIUM',
      status: 'RESOLVED',
      due: 'Met SLA'
    },
    {
      company: 'QuantumCart',
      title: 'Inventory sync job stalled overnight',
      priority: 'HIGH',
      status: 'IN PROGRESS',
      due: '6h 05m'
    },
    {
      company: 'StudioNine',
      title: 'Login webhook retries spiking',
      priority: 'LOW',
      status: 'OPEN',
      due: '2d 18h'
    }
  ];

  readonly metrics = [
    { value: '15', label: 'Seeded demo tickets' },
    { value: '4h', label: 'Urgent SLA target' },
    { value: '3', label: 'Built-in roles' }
  ];

  readonly features = [
    {
      eyebrow: 'Triage',
      title: 'Keep incidents moving',
      copy: 'Filter, assign, comment, and update ticket status from one focused workspace.'
    },
    {
      eyebrow: 'Alerts',
      title: 'Notify the right people',
      copy: 'Assignment, comment, and status-change alerts are persisted while lightweight async services keep requests quick.'
    },
    {
      eyebrow: 'Analytics',
      title: 'See the SLA picture',
      copy: 'Dashboard summaries group tickets by status, priority, workload, and resolution time.'
    }
  ];

  readonly steps = [
    {
      number: '01',
      title: 'Customers open tickets',
      copy: 'New incidents capture title, description, priority, and automatic SLA due dates.'
    },
    {
      number: '02',
      title: 'Admins assign ownership',
      copy: 'Agents get assignment alerts and see the tickets they are responsible for.'
    },
    {
      number: '03',
      title: 'Teams resolve and report',
      copy: 'Status changes, comments, and dashboard metrics make the demo feel like a real support workflow.'
    }
  ];
}
