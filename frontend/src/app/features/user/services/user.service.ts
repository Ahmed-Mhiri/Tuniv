import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { LeaderboardUser, UserCommunity, UserProfile, UserProfileUpdateRequest } from '../../../shared/models/user.model';
import { UserActivityItem } from '../../../shared/models/activity.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly http = inject(HttpClient);
  // --- IMPROVEMENT ---
  // Centralize the base URL for the user controller for consistency
  private readonly userApiUrl = `${environment.apiUrl}/users`;

  /**
   * Fetches the profile of the currently authenticated user.
   * Corresponds to: GET /api/v1/users/me
   */
  getCurrentUserProfile(): Observable<UserProfile> {
    // Corrected path
    return this.http.get<UserProfile>(`${this.userApiUrl}/me`);
  }

  /**
   * Updates the profile of the currently authenticated user.
   * Corresponds to: PUT /api/v1/users/me
   */
  updateCurrentUserProfile(updateData: UserProfileUpdateRequest): Observable<UserProfile> {
    // Corrected path
    return this.http.put<UserProfile>(`${this.userApiUrl}/me`, updateData);
  }

  /**
   * Fetches the public profile of any user by their ID.
   * Corresponds to: GET /api/v1/users/{userId}
   */
  getUserProfileById(userId: number): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.userApiUrl}/${userId}`);
  }

  /**
   * Fetches the activity feed for a specific user.
   * Corresponds to: GET /api/v1/users/{userId}/activity
   */
  getUserActivity(userId: number): Observable<UserActivityItem[]> {
    // Corrected path
    return this.http.get<UserActivityItem[]>(`${this.userApiUrl}/${userId}/activity`);
  }

  // --- NEW METHOD ---
  /**
   * Fetches the communities for the currently authenticated user.
   * Corresponds to: GET /api/v1/users/me/communities
   */
  getUserCommunities(): Observable<UserCommunity[]> {
    return this.http.get<UserCommunity[]>(`${this.userApiUrl}/me/communities`);
  }

  // --- NEW METHOD ---
  /**
   * Fetches the top users for the leaderboard.
   * Corresponds to: GET /api/v1/users/leaderboard
   */
  getLeaderboard(): Observable<LeaderboardUser[]> {
    return this.http.get<LeaderboardUser[]>(`${this.userApiUrl}/leaderboard`);
  }
}