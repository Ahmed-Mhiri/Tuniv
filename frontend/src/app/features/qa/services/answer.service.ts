// src/app/features/qa/services/answer.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AnswerService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  markAsSolution(answerId: number): Observable<any> {
    // The { responseType: 'text' } option has been removed
    return this.http.post(`${this.apiUrl}/answers/${answerId}/solution`, {});
  }
}