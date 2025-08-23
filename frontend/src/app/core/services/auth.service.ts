import { HttpClient } from '@angular/common/http';
import { Injectable, signal, inject, computed } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { environment } from '../../environments/environment';

// --- THIS IS THE FIX ---
// Interfaces are now imported from their own dedicated model file.
import { AuthRequest, AuthResponse, DecodedToken } from '../../shared/models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiUrl = environment.apiUrl;

  currentUser = signal<AuthResponse | null>(null);
  isUserLoggedIn = computed(() => !!this.currentUser());

  constructor() {
    this.tryAutoLogin();
  }

  login(credentials: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, credentials).pipe(
      tap(response => this.setSession(response))
    );
  }

  register(userInfo: unknown): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/auth/register`, userInfo);
  }

  logout(): void {
    localStorage.removeItem('authToken');
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  // --- THIS IS THE FIX ---
  // The getToken() method is added back for the interceptor to use synchronously.
  getToken(): string | null {
    return localStorage.getItem('authToken');
  }

  private setSession(authResult: AuthResponse): void {
    localStorage.setItem('authToken', authResult.token);
    this.currentUser.set(authResult);
  }

  private tryAutoLogin(): void {
    const token = this.getToken();
    if (token && !this.isTokenExpired(token)) {
      const decodedToken: AuthResponse = jwtDecode(token);
      this.currentUser.set(decodedToken);
    } else if (token) {
      // If token exists but is expired, clear it
      localStorage.removeItem('authToken');
    }
  }

  private isTokenExpired(token: string): boolean {
    try {
      const decoded: DecodedToken = jwtDecode(token);
      const expirationDate = decoded.exp * 1000;
      return expirationDate < new Date().getTime();
    } catch (e) {
      return true;
    }
  }
}