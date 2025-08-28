// src/app/features/qa/services/vote.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { VoteRequest } from '../../../shared/models/qa.model';

@Injectable({
  providedIn: 'root'
})
export class VoteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  voteOnQuestion(questionId: number, value: 1 | -1): Observable<unknown> {
    const payload: VoteRequest = { value };
    return this.http.post(`${this.apiUrl}/questions/${questionId}/vote`, payload, { responseType: 'text' });
  }

  voteOnAnswer(answerId: number, value: 1 | -1): Observable<unknown> {
    const payload: VoteRequest = { value };
    return this.http.post(`${this.apiUrl}/answers/${answerId}/vote`, payload, { responseType: 'text' });
  }
  
  voteOnComment(commentId: number, value: 1 | -1): Observable<unknown> {
    const payload: VoteRequest = { value };
    return this.http.post(`${this.apiUrl}/comments/${commentId}/vote`, payload, { responseType: 'text' });
  }
}