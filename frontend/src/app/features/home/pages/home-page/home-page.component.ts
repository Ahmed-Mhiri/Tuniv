import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

// --- Services ---

// --- NG-ZORRO Modules ---
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzEmbedEmptyComponent, NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzListModule } from 'ng-zorro-antd/list';
import { QuestionListItemComponent } from '../../../qa/components/question-list-item/question-list-item';
import { FeedService } from '../../services/feed.service';
import { UniversityService } from '../../../university/services/university.service';
import { Question } from '../../../../shared/models/qa.model';
import { University } from '../../../../shared/models/university.model';


@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [
    CommonModule, RouterLink, QuestionListItemComponent, NzGridModule,
    NzCardModule, NzButtonModule, NzIconModule, NzSpinModule,
    NzEmptyModule, NzDividerModule, NzListModule,NzEmptyModule
  ],
  templateUrl: './home-page.component.html',
  styleUrls: ['./home-page.component.scss']
})
export class HomePageComponent implements OnInit {
  private readonly feedService = inject(FeedService);
  private readonly universityService = inject(UniversityService);

  // --- State Signals ---
  readonly feedItems = signal<Question[]>([]);
  readonly topUniversities = signal<University[]>([]);
  readonly isLoading = signal(true);
  
  // --- Pagination State ---
  private currentPage = 0;
  readonly isLoadingMore = signal(false);
  readonly hasMoreItems = signal(true);

  ngOnInit(): void {
    this.loadInitialFeed();
    this.loadTopUniversities();
  }

  loadInitialFeed(): void {
    this.isLoading.set(true);
    this.hasMoreItems.set(true);
    this.currentPage = 0;

    this.feedService.getFeed(this.currentPage).subscribe(page => {
      this.feedItems.set(page.content);
      this.hasMoreItems.set(!page.last);
      this.isLoading.set(false);
    });
  }
  
  loadMore(): void {
    if (this.isLoadingMore() || !this.hasMoreItems()) return;

    this.isLoadingMore.set(true);
    this.currentPage++;

    this.feedService.getFeed(this.currentPage).subscribe(page => {
      this.feedItems.update(currentItems => [...currentItems, ...page.content]);
      this.hasMoreItems.set(!page.last);
      this.isLoadingMore.set(false);
    });
  }
  
  loadTopUniversities(): void {
    // Fetch all universities and take the top 5 by member count
    this.universityService.getAllUniversities().subscribe(universities => {
      const sorted = universities.sort((a, b) => b.memberCount - a.memberCount);
      this.topUniversities.set(sorted.slice(0, 5));
    });
  }
}