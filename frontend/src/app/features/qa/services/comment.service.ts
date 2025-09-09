// src/app/features/qa/services/comment.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Comment, CommentCreateRequest, CommentUpdateRequest, Question, VoteRequest } from '../../../shared/models/qa.model';

@Injectable({
  providedIn: 'root'
})
export class CommentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  createComment(answerId: number, request: CommentCreateRequest, files: File[]): Observable<Question> {
    const formData = new FormData();
    formData.append('comment', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    files.forEach(file => formData.append('files', file, file.name));
    // ✅ URL updated to new nested structure
    return this.http.post<Question>(`${this.apiUrl}/answers/${answerId}/comments`, formData);
  }

  updateComment(answerId: number, commentId: number, request: CommentUpdateRequest, files: File[]): Observable<Question> {
    const formData = new FormData();
    formData.append('comment', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    files.forEach(file => formData.append('files', file, file.name));
    // ✅ URL updated to new nested structure
    return this.http.put<Question>(`${this.apiUrl}/answers/${answerId}/comments/${commentId}`, formData);
  }

  deleteComment(answerId: number, commentId: number): Observable<void> {
    // ✅ URL updated to new nested structure
    return this.http.delete<void>(`${this.apiUrl}/answers/${answerId}/comments/${commentId}`);
  }

  // MOVED FROM VOTE SERVICE
  voteOnComment(answerId: number, commentId: number, value: 1 | -1): Observable<Question> {
    const payload: VoteRequest = { value };
    // ✅ URL updated to new nested structure
    return this.http.post<Question>(`${this.apiUrl}/answers/${answerId}/comments/${commentId}/vote`, payload);
  }
}