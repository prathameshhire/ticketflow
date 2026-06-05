import { Routes } from '@angular/router';

import { authGuard } from './core/auth.guard';
import { AppLayoutComponent } from './layout/app-layout.component';
import { AlertsPageComponent } from './pages/alerts/alerts-page.component';
import { DashboardPageComponent } from './pages/dashboard/dashboard-page.component';
import { LandingPageComponent } from './pages/landing/landing-page.component';
import { LoginPageComponent } from './pages/login/login-page.component';
import { RegisterPageComponent } from './pages/register/register-page.component';
import { TicketCreatePageComponent } from './pages/ticket-create/ticket-create-page.component';
import { TicketDetailPageComponent } from './pages/ticket-detail/ticket-detail-page.component';
import { TicketListPageComponent } from './pages/ticket-list/ticket-list-page.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', component: LandingPageComponent },
  { path: 'login', component: LoginPageComponent },
  { path: 'register', component: RegisterPageComponent },
  {
    path: '',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardPageComponent },
      { path: 'tickets', component: TicketListPageComponent },
      { path: 'tickets/new', component: TicketCreatePageComponent },
      { path: 'tickets/:id', component: TicketDetailPageComponent },
      { path: 'alerts', component: AlertsPageComponent }
    ]
  },
  { path: '**', redirectTo: '' }
];
