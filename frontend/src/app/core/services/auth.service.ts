import { HttpClient } from '@angular/common/http';
import { Injectable, signal, inject, computed, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { environment } from '../../../environments/environment';
import {
  AuthRequest,
  AuthResponse,
  DecodedToken,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  TwoFactorGenerateResponse
} from '../../shared/models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly apiUrl = environment.apiUrl;

  currentUser = signal<AuthResponse | null>(null);
  isUserLoggedIn = computed(() => !!this.currentUser());

  isTwoFactorStep = signal(false);
  private pendingCredentials: AuthRequest | null = null;

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.tryAutoLogin();
    }
  }

  login(credentials: AuthRequest): Observable<AuthResponse> {
    if (this.isTwoFactorStep() && this.pendingCredentials) {
        credentials = { ...this.pendingCredentials, code: credentials.code };
    }

    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, credentials).pipe(
      tap(response => {
        if (response.is2faRequired) {
          this.isTwoFactorStep.set(true);
          this.pendingCredentials = { username: credentials.username, password: credentials.password };
        } else {
          this.setSession(response);
          this.isTwoFactorStep.set(false);
          this.pendingCredentials = null;
        }
      })
    );
  }

  register(userInfo: unknown): Observable<unknown> {
    // Return a text response because the backend sends a simple string message
    return this.http.post(`${this.apiUrl}/auth/register`, userInfo, { responseType: 'text' });
  }

  // --- NEW: Method for Email Verification ---
  verify(token: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/auth/verify`, { params: { token } });
  }

  forgotPassword(data: ForgotPasswordRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/forgot-password`, data);
  }

  resetPassword(data: ResetPasswordRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/reset-password`, data);
  }

  generate2faSecret(): Observable<TwoFactorGenerateResponse> {
    return this.http.post<TwoFactorGenerateResponse>(`${this.apiUrl}/auth/2fa/generate`, {});
  }

  enable2fa(code: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/2fa/enable`, { code });
  }

  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('authToken');
    }
    this.currentUser.set(null);
    this.isTwoFactorStep.set(false);
    this.pendingCredentials = null;
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('authToken');
    }
    return null;
  }
  
  private setSession(authResult: AuthResponse): void {
    if (authResult.token && isPlatformBrowser(this.platformId)) {
      localStorage.setItem('authToken', authResult.token);
    }
    this.currentUser.set(authResult);
  }

  private tryAutoLogin(): void {
    const token = this.getToken();
    if (token && !this.isTokenExpired(token)) {
      const userSession: AuthResponse = this.decodeTokenToAuthResponse(token);
      this.currentUser.set(userSession);
    } else if (token) {
      localStorage.removeItem('authToken');
    }
  }

  private decodeTokenToAuthResponse(token: string): AuthResponse {
    const decoded: DecodedToken = jwtDecode(token);
    return {
      token: token,
      id: decoded.id,
      username: decoded.sub,
      email: decoded.email
    };
  }

  hasRole(role: string): boolean {
    const token = this.getToken();
    if (!token) return false;
    const decoded: DecodedToken = jwtDecode(token);
    return decoded.roles?.includes(role) ?? false;
  }

  private isTokenExpired(token: string): boolean {
    try {
      const decoded: DecodedToken = jwtDecode(token);
      if (!decoded.exp) return true;
      const expirationDate = decoded.exp * 1000;
      return expirationDate < new Date().getTime();
    } catch (e) {
      return true;
    }
  }
}
