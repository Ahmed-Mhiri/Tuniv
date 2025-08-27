import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { University, Module } from '../../../shared/models/university.model';

@Injectable({
  providedIn: 'root'
})
export class UniversityService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * Fetches all universities and their modules.
   * Corresponds to: GET /api/v1/universities
   */
  getAllUniversities(): Observable<University[]> {
    return this.http.get<University[]>(`${this.apiUrl}/universities`);
  }

  /**
   * Fetches all modules for a specific university.
   * Corresponds to: GET /api/v1/universities/{universityId}/modules
   */
  getModulesByUniversity(universityId: number): Observable<Module[]> {
    return this.http.get<Module[]>(`${this.apiUrl}/universities/${universityId}/modules`);
  }

  /**
   * Allows the currently logged-in user to join a university.
   * Corresponds to: POST /api/v1/universities/{universityId}/members
   */
  joinUniversity(universityId: number): Observable<unknown> {
    // This POST request sends no body, as the backend identifies
    // the user from the JWT token provided by the interceptor.
    return this.http.post(`${this.apiUrl}/universities/${universityId}/members`, {});
  }

  unjoinUniversity(universityId: number): Observable<unknown> {
    return this.http.delete(`${this.apiUrl}/universities/${universityId}/members`);
  }
}