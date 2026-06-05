import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, of, tap } from 'rxjs';

import { ApiService } from './api.service';
import { AuthResponse, CurrentUser, LoginRequest, RegisterRequest } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'ticketflow.auth.token';
  private readonly userKey = 'ticketflow.auth.user';
  private readonly userSubject = new BehaviorSubject<CurrentUser | null>(this.getStoredUser());

  readonly currentUser$ = this.userSubject.asObservable();

  constructor(private readonly api: ApiService) {
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.api.login(request).pipe(tap((response) => this.storeAuth(response)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.api.register(request).pipe(tap((response) => this.storeAuth(response)));
  }

  refreshCurrentUser(): Observable<CurrentUser | null> {
    if (!this.getToken()) {
      return of(null);
    }

    return this.api.me().pipe(
      tap((user) => this.storeUser(user)),
      catchError(() => {
        this.logout();
        return of(null);
      })
    );
  }

  get currentUser(): CurrentUser | null {
    return this.userSubject.value;
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return Boolean(this.getToken());
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.userSubject.next(null);
  }

  private storeAuth(response: AuthResponse): void {
    localStorage.setItem(this.tokenKey, response.token);
    this.storeUser(response.user);
  }

  private storeUser(user: CurrentUser): void {
    localStorage.setItem(this.userKey, JSON.stringify(user));
    this.userSubject.next(user);
  }

  private getStoredUser(): CurrentUser | null {
    const value = localStorage.getItem(this.userKey);
    if (!value) {
      return null;
    }

    try {
      return JSON.parse(value) as CurrentUser;
    } catch {
      localStorage.removeItem(this.userKey);
      return null;
    }
  }
}
