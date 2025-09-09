import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Page } from '../../../shared/models/pagination.model';
import { Question, QuestionSummaryDto } from '../../../shared/models/qa.model';


@Injectable({
  providedIn: 'root'
})
export class FeedService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * Fetches the personalized feed for the logged-in user.
   */
  // ✅ 2. Update the return type
  getPersonalizedFeed(page: number = 0, size: number = 10): Observable<Page<QuestionSummaryDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');

    // ✅ 3. Update the generic type for the HTTP call
    return this.http.get<Page<QuestionSummaryDto>>(`${this.apiUrl}/feed`, { params });
  }

  /**
   * Fetches the popular feed.
   */
  // ✅ 2. Update the return type
  getPopularFeed(page: number = 0, size: number = 10): Observable<Page<QuestionSummaryDto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    // ✅ 3. Update the generic type for the HTTP call
    return this.http.get<Page<QuestionSummaryDto>>(`${this.apiUrl}/feed/popular`, { params });
  }
}