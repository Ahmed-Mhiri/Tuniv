import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { UserProfile, UserProfileUpdateRequest } from '../../../shared/models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * Fetches the profile of the currently authenticated user.
   * Corresponds to: GET /api/v1/me
   */
  getCurrentUserProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/me`);
  }

  /**
   * Updates the profile of the currently authenticated user.
   * Corresponds to: PUT /api/v1/me
   */
  updateCurrentUserProfile(updateData: UserProfileUpdateRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/me`, updateData);
  }

  /**
   * Fetches the public profile of any user by their ID.
   * Corresponds to: GET /api/v1/users/{userId}
   */
  getUserProfileById(userId: number): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/users/${userId}`);
  }
}