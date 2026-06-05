import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { getApiBaseUrl } from './runtime-env';
import {
  Alert,
  AuthResponse,
  CurrentUser,
  DashboardSummary,
  LoginRequest,
  PageResponse,
  ReadAllAlertsResponse,
  RegisterRequest,
  Ticket,
  TicketComment,
  TicketFilters,
  TicketPriority,
  TicketStatus
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = getApiBaseUrl();

  constructor(private readonly http: HttpClient) {
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.url('/auth/login'), request);
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.url('/auth/register'), request);
  }

  me(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(this.url('/auth/me'));
  }

  dashboardSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(this.url('/dashboard/summary'));
  }

  listTickets(filters: TicketFilters): Observable<PageResponse<Ticket>> {
    let params = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 20))
      .set('sort', filters.sort ?? 'createdAt');

    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.priority) {
      params = params.set('priority', filters.priority);
    }
    if (filters.assignedAgentId) {
      params = params.set('assignedAgentId', String(filters.assignedAgentId));
    }
    if (filters.customerId) {
      params = params.set('customerId', String(filters.customerId));
    }
    if (filters.q?.trim()) {
      params = params.set('q', filters.q.trim());
    }

    return this.http.get<PageResponse<Ticket>>(this.url('/tickets'), { params });
  }

  createTicket(request: { title: string; description: string; priority: TicketPriority }): Observable<Ticket> {
    return this.http.post<Ticket>(this.url('/tickets'), request);
  }

  getTicket(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(this.url(`/tickets/${id}`));
  }

  updateTicketStatus(id: number, status: TicketStatus): Observable<Ticket> {
    return this.http.patch<Ticket>(this.url(`/tickets/${id}/status`), { status });
  }

  assignTicket(id: number, assignedAgentId: number | null): Observable<Ticket> {
    return this.http.patch<Ticket>(this.url(`/tickets/${id}/assign`), { assignedAgentId });
  }

  listComments(ticketId: number): Observable<TicketComment[]> {
    return this.http.get<TicketComment[]>(this.url(`/tickets/${ticketId}/comments`));
  }

  addComment(ticketId: number, body: string): Observable<TicketComment> {
    return this.http.post<TicketComment>(this.url(`/tickets/${ticketId}/comments`), { body });
  }

  listAlerts(): Observable<Alert[]> {
    return this.http.get<Alert[]>(this.url('/alerts'));
  }

  markAlertRead(id: number): Observable<Alert> {
    return this.http.patch<Alert>(this.url(`/alerts/${id}/read`), {});
  }

  markAllAlertsRead(): Observable<ReadAllAlertsResponse> {
    return this.http.patch<ReadAllAlertsResponse>(this.url('/alerts/read-all'), {});
  }

  private url(path: string): string {
    return `${this.baseUrl}${path}`;
  }
}
