export type UserRole = 'ADMIN' | 'AGENT' | 'CUSTOMER';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type AlertType = 'ASSIGNMENT' | 'SLA_WARNING' | 'STATUS_CHANGE' | 'COMMENT';

export interface CurrentUser {
  id: number;
  name: string;
  email: string;
  role: UserRole;
}

export interface AuthResponse {
  token: string;
  tokenType: 'Bearer';
  user: CurrentUser;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface UserSummary {
  id: number;
  name: string;
  email: string;
  role: UserRole;
}

export interface Ticket {
  id: number;
  title: string;
  description: string;
  priority: TicketPriority;
  status: TicketStatus;
  customer: UserSummary;
  assignedAgent: UserSummary | null;
  slaDueAt: string;
  resolvedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TicketComment {
  id: number;
  ticketId: number;
  author: UserSummary;
  body: string;
  createdAt: string;
}

export interface Alert {
  id: number;
  recipient: UserSummary;
  type: AlertType;
  message: string;
  readFlag: boolean;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface TicketFilters {
  page?: number;
  size?: number;
  status?: TicketStatus | '';
  priority?: TicketPriority | '';
  assignedAgentId?: number | null;
  customerId?: number | null;
  q?: string;
  sort?: 'createdAt' | 'priority';
}

export interface AgentWorkload {
  agentId: number | null;
  agentName: string;
  agentEmail: string;
  totalAssigned: number;
  openCount: number;
  inProgressCount: number;
}

export interface DashboardSummary {
  totalTickets: number;
  openCount: number;
  inProgressCount: number;
  resolvedCount: number;
  closedCount: number;
  overdueSlaCount: number;
  ticketsByPriority: Partial<Record<TicketPriority, number>>;
  ticketsByStatus: Partial<Record<TicketStatus, number>>;
  agentWorkload: AgentWorkload[];
  averageResolutionTimeHours: number;
}

export interface ReadAllAlertsResponse {
  updatedCount: number;
}
