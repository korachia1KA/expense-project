import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, User } from '../models';

/**
 * localStorage keys. The "expensetracker." prefix avoids clashes with other apps served from the
 * same origin (e.g. localhost). The token survives page refreshes until the user logs out.
 */
export const TOKEN_KEY = 'expensetracker.token';
export const USER_KEY = 'expensetracker.user';

/**
 * Owns everything about "who is logged in": login, register, logout and the stored JWT.
 *
 * `@Injectable({ providedIn: 'root' })` registers ONE shared instance (a singleton) for the whole
 * app. Any component or service can get it with `inject(AuthService)`.
 *
 * State lives in Angular SIGNALS. A signal is a reactive value: reading it inside a template or a
 * `computed()` creates a dependency, and when it changes Angular re-renders exactly what depends on
 * it. When login() sets `_currentUser`, the navbar appears on its own; no manual refresh code.
 *
 * Security note: localStorage is readable by any JavaScript on the page, so an XSS bug could steal
 * the token. Angular escapes template output by default, which mitigates XSS. The alternative
 * (httpOnly cookies) brings back CSRF concerns; see docs/HOW-IT-WORKS.md for the trade-off.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  // inject() is the modern alternative to constructor parameters for dependency injection.
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /** signal(initialValue) creates a WRITABLE signal. It's private so only this service can change it. */
  private readonly _currentUser = signal<User | null>(this.readStoredUser());

  /** Read-only view for components: they can read currentUser() but not set it. */
  readonly currentUser = this._currentUser.asReadonly();

  /** computed() derives a signal from others and recalculates automatically when they change. */
  readonly isLoggedIn = computed(() => this._currentUser() !== null);

  /** The raw JWT, read by the HTTP interceptor for every request. */
  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', { email, password })
      // tap() runs a side effect (saving the session) without changing the emitted value.
      .pipe(tap((res) => this.storeSession(res)));
  }

  /** Registering also logs in: the backend returns a token and creates the default categories. */
  register(name: string, email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', { name, email, password })
      .pipe(tap((res) => this.storeSession(res)));
  }

  /** Clears the session and goes to /login. JWTs are stateless, so there is no server call to make. */
  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this._currentUser.set(null);
    this.router.navigate(['/login']);
  }

  private storeSession(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this._currentUser.set(res.user);
  }

  /** Restores the user after a page refresh (only if a token is stored too). */
  private readStoredUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw || !localStorage.getItem(TOKEN_KEY)) {
      return null;
    }
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null; // corrupted value: treat as logged out
    }
  }
}
