import { AsyncPipe, NgIf } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../core/auth.service';

@Component({
  selector: 'tf-app-layout',
  standalone: true,
  imports: [AsyncPipe, NgIf, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app-layout.component.html'
})
export class AppLayoutComponent implements OnInit {
  readonly currentUser$ = this.auth.currentUser$;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router
  ) {
  }

  ngOnInit(): void {
    this.auth.refreshCurrentUser().subscribe();
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }

  canCreateTicket(role: string): boolean {
    return role === 'CUSTOMER';
  }
}
