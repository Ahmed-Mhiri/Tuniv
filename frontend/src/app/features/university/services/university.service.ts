// src/app/features/universities/services/university.service.ts
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

  // --- METHOD REMOVED ---
  // The getModulesByUniversity method has been moved to ModuleService.

  /**
   * Allows the currently logged-in user to join a university.
   * Corresponds to: POST /api/v1/universities/{universityId}/members
   */
  joinUniversity(universityId: number): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/universities/${universityId}/members`, {});
  }

  /**
   * Allows the currently logged-in user to unjoin a university.
   * Corresponds to: DELETE /api/v1/universities/{universityId}/members
   */
  unjoinUniversity(universityId: number): Observable<unknown> {
    return this.http.delete(`${this.apiUrl}/universities/${universityId}/members`);
  }
}