// src/app/features/universities/services/module.service.ts
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Module, ModuleDetail } from '../../../shared/models/university.model';
import { Page, Pageable } from '../../../shared/models/pagination.model';

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
    getModulesByUniversity(universityId: number, pageable: Pageable): Observable<Page<Module>> {
    // Create HttpParams to send page, size, and sort info to the backend
    const params = new HttpParams()
      .set('page', pageable.page.toString())
      .set('size', pageable.size.toString())
      .set('sort', pageable.sort || 'name,asc'); // Default sort if not provided

    return this.http.get<Page<Module>>(`${this.apiUrl}/universities/${universityId}/modules`, { params });
  }

  /**
   * Fetches a paginated list of all modules in the system.
   */
  getAllModules(pageable: Pageable): Observable<Page<Module>> {
    const params = new HttpParams()
      .set('page', pageable.page.toString())
      .set('size', pageable.size.toString())
      .set('sort', pageable.sort || 'name,asc');

    return this.http.get<Page<Module>>(`${this.apiUrl}/modules`, { params });
  }
  getModuleById(moduleId: number): Observable<ModuleDetail> {
  return this.http.get<ModuleDetail>(`${this.apiUrl}/modules/${moduleId}`);
}
getModulesForDropdown(universityId: number): Observable<Module[]> {
  return this.http.get<Module[]>(`${this.apiUrl}/universities/${universityId}/modules/all`);
}




}