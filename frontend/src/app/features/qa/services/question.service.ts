// src/app/features/qa/services/question.service.ts
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { environment } from '../../../../environments/environment';
// --- FIX: Import the DTOs needed for creating questions and answers ---
import { 
  Answer, 
  AnswerCreateRequest, 
  AnswerUpdateRequest, 
  Question, 
  QuestionCreateRequest, 
  QuestionResponseDto, 
  QuestionSummaryDto, 
  QuestionUpdateRequest,
  VoteRequest
} from '../../../shared/models/qa.model';
import { Page } from '../../../shared/models/pagination.model';

@Injectable({
  providedIn: 'root'
})

export class QuestionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/questions`; // Base path for questions

  // =================================================================
  // == Question Methods
  // =================================================================

  getQuestionById(questionId: number): Observable<Question> {
    return this.http.get<Question>(`${this.apiUrl}/${questionId}`);
  }

  createQuestion(request: QuestionCreateRequest, files: File[]): Observable<QuestionResponseDto> {
    const formData = new FormData();
    formData.append('question', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    files.forEach(file => formData.append('files', file, file.name));
    return this.http.post<QuestionResponseDto>(this.apiUrl, formData);
  }

  updateQuestion(questionId: number, request: QuestionUpdateRequest, files: File[]): Observable<Question> {
    const formData = new FormData();
    formData.append('question', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    files.forEach(file => formData.append('files', file, file.name));
    return this.http.put<Question>(`${this.apiUrl}/${questionId}`, formData);
  }

  deleteQuestion(questionId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${questionId}`);
  }

  // MOVED FROM VOTE SERVICE
  voteOnQuestion(questionId: number, value: 1 | -1): Observable<Question> {
    const payload: VoteRequest = { value };
    return this.http.post<Question>(`${this.apiUrl}/${questionId}/vote`, payload);
  }

  // =================================================================
  // == List/Query Methods
  // =================================================================

  /**
   * ✅ NEW METHOD
   * Gets a paginated list of question summaries, filtered by module.
   */
  getQuestionsByModule(
    moduleId: number,
    page: number = 0,
    size: number = 10
  ): Observable<Page<QuestionSummaryDto>> {
    const params = new HttpParams()
      .set('moduleId', moduleId.toString())
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');
    return this.http.get<Page<QuestionSummaryDto>>(this.apiUrl, { params });
  }

  // =================================================================
  // == Answer Methods (as sub-resources of Questions)
  // =================================================================

  addAnswer(questionId: number, answerData: AnswerCreateRequest, files: File[]): Observable<Question> {
    const formData = new FormData();
    formData.append('answer', new Blob([JSON.stringify(answerData)], { type: 'application/json' }));
    files.forEach(file => formData.append('files', file, file.name));
    return this.http.post<Question>(`${this.apiUrl}/${questionId}/answers`, formData);
  }

  // MOVED FROM ANSWER SERVICE
  updateAnswer(questionId: number, answerId: number, request: AnswerUpdateRequest, files: File[]): Observable<Question> {
    const formData = new FormData();
    formData.append('answer', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    files.forEach(file => formData.append('files', file, file.name));
    return this.http.put<Question>(`${this.apiUrl}/${questionId}/answers/${answerId}`, formData);
  }

  // MOVED FROM ANSWER SERVICE
  deleteAnswer(questionId: number, answerId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${questionId}/answers/${answerId}`);
  }

  // MOVED FROM ANSWER SERVICE
  markAsSolution(questionId: number, answerId: number): Observable<Question> {
    return this.http.post<Question>(`${this.apiUrl}/${questionId}/answers/${answerId}/solution`, null);
  }

  // MOVED FROM VOTE SERVICE
  voteOnAnswer(questionId: number, answerId: number, value: 1 | -1): Observable<Question> {
    const payload: VoteRequest = { value };
    return this.http.post<Question>(`${this.apiUrl}/${questionId}/answers/${answerId}/vote`, payload);
  }

  // =================================================================
  // == Other Methods
  // =================================================================

  findQuestionIdByAnswerId(answerId: number): Observable<number> {
    return this.http.get<number>(`${environment.apiUrl}/answers/${answerId}/question-id`);
  }

  findQuestionIdByCommentId(commentId: number): Observable<number> {
    return this.http.get<number>(`${environment.apiUrl}/comments/${commentId}/question-id`);
  }
  searchQuestions(query: string, page: number = 0, size: number = 5): Observable<Page<Question>> {
    // TODO: Implement the actual API call to your backend's search endpoint.
    // For now, it returns an empty result to prevent errors.
    console.warn('searchQuestions() is not yet implemented. Returning empty results.');

    // ✅ FIX: Create a mock empty Page object and return it as an Observable
    const emptyPage: Page<Question> = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: page,
      size: size,
      first: true,
      last: true,
      empty: true,
      numberOfElements: 0,
      sort: { sorted: false, unsorted: true, empty: true },
    };

    return of(emptyPage);
  }
}