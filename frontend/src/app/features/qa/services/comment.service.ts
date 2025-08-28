// src/app/features/qa/services/comment.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Comment, CommentCreateRequest } from '../../../shared/models/qa.model';

@Injectable({
  providedIn: 'root'
})
export class CommentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  createComment(answerId: string, request: CommentCreateRequest, files: File[]): Observable<Comment> {
    const formData = new FormData();
    formData.append('comment', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    
    files.forEach(file => {
      formData.append('files', file, file.name);
    });

    return this.http.post<Comment>(`${this.apiUrl}/answers/${answerId}/comments`, formData);
  }
}