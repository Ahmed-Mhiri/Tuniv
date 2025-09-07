import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Page } from '../../../shared/models/pagination.model';
import { Question } from '../../../shared/models/qa.model';


@Injectable({
  providedIn: 'root'
})
export class FeedService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * Fetches the personalized feed for the logged-in user.
   * Corresponds to: GET /api/v1/feed
   */
  getFeed(page: number = 0, size: number = 10): Observable<Page<Question>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc'); // Sort by newest questions

    return this.http.get<Page<Question>>(`${this.apiUrl}/feed`, { params });
  }

  getPopularFeed(page: number = 0, size: number = 10): Observable<Page<Question>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    // No sort needed, the backend 'hot' algorithm handles it
    return this.http.get<Page<Question>>(`${this.apiUrl}/feed/popular`, { params });
  }
}