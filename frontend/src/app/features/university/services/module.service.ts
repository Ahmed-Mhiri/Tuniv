// src/app/features/universities/services/module.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Module } from '../../../shared/models/university.model';

@Injectable({
  providedIn: 'root'
})
export class ModuleService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * Fetches a flat list of all modules in the system.
   * Corresponds to: GET /api/v1/modules
   */
  getAllModules(): Observable<Module[]> {
    return this.http.get<Module[]>(`${this.apiUrl}/modules`);
  }

  /**
   * Fetches all modules for a specific university.
   * Corresponds to: GET /api/v1/universities/{universityId}/modules
   */
  getModulesByUniversity(universityId: number): Observable<Module[]> {
    return this.http.get<Module[]>(`${this.apiUrl}/universities/${universityId}/modules`);
  }
}