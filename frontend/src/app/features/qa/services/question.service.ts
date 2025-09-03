// src/app/features/qa/services/question.service.ts
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
// --- FIX: Import the DTOs needed for creating questions and answers ---
import { 
  Answer, 
  AnswerCreateRequest, 
  Question, 
  QuestionCreateRequest, 
  QuestionResponseDto 
} from '../../../shared/models/qa.model';
import { Page } from '../../../shared/models/pagination.model';

@Injectable({
  providedIn: 'root',
})
export class QuestionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getQuestionById(questionId: number): Observable<Question> {
    return this.http.get<Question>(`${this.apiUrl}/questions/${questionId}`);
  }

  // ===============================================================
  // ✅ NEW METHOD ADDED
  // ===============================================================
  /**
   * Fetches a paginated list of questions for a specific module.
   * Corresponds to: GET /api/v1/modules/{moduleId}/questions
   */
  getQuestionsByModule(moduleId: number, page: number = 0, size: number = 10): Observable<Page<Question>> {
    // Use HttpParams to safely add URL query parameters for pagination and sorting
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc'); // Default sort: newest questions first

    const url = `${this.apiUrl}/modules/${moduleId}/questions`;
    return this.http.get<Page<Question>>(url, { params });
  }

  createQuestion(
    moduleId: number,
    questionData: QuestionCreateRequest,
    files: File[]
  ): Observable<QuestionResponseDto> {
    const formData = new FormData();
    formData.append('question', new Blob([JSON.stringify(questionData)], { type: 'application/json' }));

    files.forEach(file => {
      formData.append('files', file, file.name);
    });
    
    const url = `${this.apiUrl}/modules/${moduleId}/questions`;
    return this.http.post<QuestionResponseDto>(url, formData);
  }

  addAnswer(questionId: number, answerData: AnswerCreateRequest, files: File[]): Observable<Answer> {
  const formData = new FormData();
  formData.append('answer', new Blob([JSON.stringify(answerData)], { type: 'application/json' }));
  
  files.forEach(file => {
    formData.append('files', file, file.name);
  });

  // ✅ ADD THIS DEBUGGING BLOCK
  console.log('--- DEBUG: Inspecting FormData in QuestionService ---');
  for (const [key, value] of formData.entries()) {
    console.log(`Key: '${key}', Value:`, value);
  }
  console.log('--------------------------------------------------');

  return this.http.post<Answer>(`${this.apiUrl}/questions/${questionId}/answers`, formData);
}
  searchQuestions(query: string, page: number = 0, size: number = 5): Observable<Page<Question>> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<Question>>(`${this.apiUrl}/questions/search`, { params });
  }
}