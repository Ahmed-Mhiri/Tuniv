// src/app/features/qa/services/question.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Question, Answer } from '../../../shared/models/qa.model';

@Injectable({
  providedIn: 'root',
})
export class QuestionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getQuestionById(questionId: number): Observable<Question> {
    return this.http.get<Question>(`${this.apiUrl}/questions/${questionId}`);
  }

  addAnswer(questionId: number, body: string, files: File[]): Observable<Answer> {
    const formData = new FormData();
    formData.append('answer', new Blob([JSON.stringify({ body })], { type: 'application/json' }));
    
    files.forEach(file => {
      formData.append('files', file, file.name);
    });

    return this.http.post<Answer>(`${this.apiUrl}/questions/${questionId}/answers`, formData);
  }
}