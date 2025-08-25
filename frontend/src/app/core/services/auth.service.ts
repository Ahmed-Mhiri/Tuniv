import { HttpClient } from '@angular/common/http';
import { Injectable, signal, inject, computed, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common'; // <-- IMPORT ADDED
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { environment } from '../../../environments/environment';
import { AuthRequest, AuthResponse, DecodedToken } from '../../shared/models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID); // <-- INJECT PLATFORM_ID
  private readonly apiUrl = environment.apiUrl;

  currentUser = signal<AuthResponse | null>(null);
  isUserLoggedIn = computed(() => !!this.currentUser());

  constructor() {
    // Only try to auto-login if we are in a browser environment
    if (isPlatformBrowser(this.platformId)) {
      this.tryAutoLogin();
    }
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
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('authToken');
    }
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('authToken');
    }
    return null;
  }

  private setSession(authResult: AuthResponse): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('authToken', authResult.token);
    }
    this.currentUser.set(authResult);
  }

  private tryAutoLogin(): void {
    const token = this.getToken();
    if (token && !this.isTokenExpired(token)) {
      const decodedToken: AuthResponse = jwtDecode(token);
      this.currentUser.set(decodedToken);
    } else if (token) {
      localStorage.removeItem('authToken');
    }
  }

  private isTokenExpired(token: string): boolean {
    // ... (this method is safe as it doesn't use browser APIs)
    try {
      const decoded: DecodedToken = jwtDecode(token);
      const expirationDate = decoded.exp * 1000;
      return expirationDate < new Date().getTime();
    } catch (e) {
      return true;
    }
  }
}