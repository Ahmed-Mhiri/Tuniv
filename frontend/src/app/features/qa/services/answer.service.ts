// src/app/features/qa/services/answer.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Answer, AnswerUpdateRequest } from '../../../shared/models/qa.model';

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
  updateAnswer(
    answerId: number,
    request: AnswerUpdateRequest,
    files: File[]
  ): Observable<Answer> {
    const formData = new FormData();
    formData.append('answer', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    files.forEach(file => formData.append('files', file, file.name));

    return this.http.put<Answer>(`${this.apiUrl}/answers/${answerId}`, formData);
  }

  // ✨ --- NEW: DELETE AN ANSWER --- ✨
  deleteAnswer(answerId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/answers/${answerId}`);
  }
}