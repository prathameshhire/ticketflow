import { DatePipe, NgClass, NgFor, NgIf } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AgentWorkload, CurrentUser, Ticket, TicketComment, TicketStatus, UserSummary } from '../../core/models';

interface AgentOption {
  id: number;
  name: string;
  email: string;
}

@Component({
  selector: 'tf-ticket-detail-page',
  standalone: true,
  imports: [DatePipe, NgClass, NgFor, NgIf, ReactiveFormsModule, RouterLink],
  templateUrl: './ticket-detail-page.component.html'
})
export class TicketDetailPageComponent implements OnInit {
  ticket: Ticket | null = null;
  comments: TicketComment[] = [];
  agentOptions: AgentOption[] = [];
  currentUser: CurrentUser | null = null;
  errorMessage = '';
  successMessage = '';
  isTicketLoading = false;
  isCommentsLoading = false;
  isLoadingAgents = false;
  isUpdatingStatus = false;
  isAssigning = false;
  isAddingComment = false;
  readonly statuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

  readonly statusForm = this.fb.nonNullable.group({
    status: ['OPEN' as TicketStatus, [Validators.required]]
  });

  readonly assignForm = this.fb.nonNullable.group({
    assignedAgentId: ['']
  });

  readonly commentForm = this.fb.nonNullable.group({
    body: ['', [Validators.required, Validators.maxLength(3000)]]
  });

  private ticketId = 0;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly fb: FormBuilder,
    private readonly api: ApiService,
    private readonly auth: AuthService
  ) {
  }

  ngOnInit(): void {
    this.ticketId = Number(this.route.snapshot.paramMap.get('id'));
    this.currentUser = this.auth.currentUser;
    this.loadTicket();
    this.loadComments();
    if (this.canAssign()) {
      this.loadAgentOptions();
    }
  }

  updateStatus(): void {
    if (!this.ticket || this.statusForm.invalid || !this.canManageStatus(this.ticket)) {
      return;
    }

    this.clearMessages();
    this.isUpdatingStatus = true;
    this.api.updateTicketStatus(this.ticket.id, this.statusForm.getRawValue().status).subscribe({
      next: (ticket) => {
        this.ticket = ticket;
        this.successMessage = 'Status updated.';
        this.isUpdatingStatus = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Unable to update status.';
        this.isUpdatingStatus = false;
      }
    });
  }

  assignTicket(): void {
    if (!this.ticket || !this.canAssign()) {
      return;
    }

    const rawAgentId = this.assignForm.getRawValue().assignedAgentId;
    const assignedAgentId = rawAgentId ? Number(rawAgentId) : null;
    this.clearMessages();
    this.isAssigning = true;
    this.api.assignTicket(this.ticket.id, assignedAgentId).subscribe({
      next: (ticket) => {
        this.ticket = ticket;
        this.includeAssignedAgent(ticket.assignedAgent);
        this.successMessage = 'Assignment updated.';
        this.isAssigning = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Unable to assign ticket.';
        this.isAssigning = false;
      }
    });
  }

  addComment(): void {
    if (!this.ticket || this.commentForm.invalid) {
      this.commentForm.markAllAsTouched();
      return;
    }

    const body = this.commentForm.getRawValue().body.trim();
    if (!body) {
      return;
    }

    this.clearMessages();
    this.isAddingComment = true;
    this.api.addComment(this.ticket.id, body).subscribe({
      next: (comment) => {
        this.comments = [...this.comments, comment];
        this.commentForm.reset({ body: '' });
        this.isAddingComment = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Unable to add comment.';
        this.isAddingComment = false;
      }
    });
  }

  canManageStatus(ticket: Ticket | null = this.ticket): boolean {
    if (this.currentUser?.role === 'ADMIN') {
      return true;
    }

    return this.currentUser?.role === 'AGENT' && ticket?.assignedAgent?.id === this.currentUser.id;
  }

  canAssign(): boolean {
    return this.currentUser?.role === 'ADMIN';
  }

  private loadTicket(): void {
    this.isTicketLoading = true;
    this.api.getTicket(this.ticketId).subscribe({
      next: (ticket) => {
        this.ticket = ticket;
        this.statusForm.patchValue({ status: ticket.status });
        this.assignForm.patchValue({ assignedAgentId: ticket.assignedAgent?.id ? String(ticket.assignedAgent.id) : '' });
        this.includeAssignedAgent(ticket.assignedAgent);
        this.isTicketLoading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load ticket.';
        this.isTicketLoading = false;
      }
    });
  }

  private loadComments(): void {
    this.isCommentsLoading = true;
    this.api.listComments(this.ticketId).subscribe({
      next: (comments) => {
        this.comments = comments;
        this.isCommentsLoading = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load comments.';
        this.isCommentsLoading = false;
      }
    });
  }

  private loadAgentOptions(): void {
    this.isLoadingAgents = true;
    this.api.dashboardSummary().subscribe({
      next: (summary) => {
        this.agentOptions = summary.agentWorkload
          .filter((agent): agent is AgentWorkload & { agentId: number } => agent.agentId !== null)
          .map((agent) => ({
            id: agent.agentId,
            name: agent.agentName,
            email: agent.agentEmail
          }))
          .sort((left, right) => left.name.localeCompare(right.name));
        this.includeAssignedAgent(this.ticket?.assignedAgent ?? null);
        this.isLoadingAgents = false;
      },
      error: () => {
        this.errorMessage = 'Unable to load agent assignment options.';
        this.isLoadingAgents = false;
      }
    });
  }

  private includeAssignedAgent(agent: UserSummary | null): void {
    if (!agent || agent.role !== 'AGENT') {
      return;
    }

    if (this.agentOptions.some((option) => option.id === agent.id)) {
      return;
    }

    this.agentOptions = [...this.agentOptions, {
      id: agent.id,
      name: agent.name,
      email: agent.email
    }].sort((left, right) => left.name.localeCompare(right.name));
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
