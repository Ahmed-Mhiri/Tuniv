import { Component, OnInit, TemplateRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

// --- Services ---
import { FeedService } from '../../services/feed.service';
import { UniversityService } from '../../../university/services/university.service';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService } from '../../../user/services/user.service'; // ✅ IMPORT UserService

// --- Models ---
import { Question } from '../../../../shared/models/qa.model';
import { University } from '../../../../shared/models/university.model';
import { LeaderboardUser, UserCommunity } from '../../../../shared/models/user.model'; // ✅ IMPORT new models

// --- Components ---
import { QuestionListItemComponent } from '../../../qa/components/question-list-item/question-list-item';

// --- NG-ZORRO Modules ---
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzListModule } from 'ng-zorro-antd/list';
import { NzModalModule, NzModalRef, NzModalService } from 'ng-zorro-antd/modal';
import { NzAvatarModule } from 'ng-zorro-antd/avatar'; // ✅ IMPORT NzAvatarModule


@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [
    CommonModule, RouterLink, QuestionListItemComponent, NzGridModule,
    NzCardModule, NzButtonModule, NzIconModule, NzSpinModule,
    NzEmptyModule, NzDividerModule, NzListModule, NzModalModule, NzAvatarModule // ✅ ADD NzAvatarModule
  ],
  templateUrl: './home-page.component.html',
  styleUrls: ['./home-page.component.scss']
})
export class HomePageComponent implements OnInit {
  // --- Services ---
  private readonly feedService = inject(FeedService);
  private readonly universityService = inject(UniversityService);
  private readonly userService = inject(UserService); // ✅ INJECT UserService
  public readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly modal = inject(NzModalService);


  // --- State Signals ---
  readonly feedItems = signal<Question[]>([]);
  readonly topUniversities = signal<University[]>([]);
  readonly isLoading = signal(true);
  readonly myCommunities = signal<UserCommunity[]>([]);   // ✅ NEW Signal
  readonly leaderboard = signal<LeaderboardUser[]>([]);     // ✅ NEW Signal
  
  // --- Pagination State ---
  private currentPage = 0;
  readonly isLoadingMore = signal(false);
  readonly hasMoreItems = signal(true);
  private activeFeedType = signal<'PERSONALIZED' | 'POPULAR'>('POPULAR');
  private activeModal: NzModalRef | null = null;


  ngOnInit(): void {
    // Load content based on login status
    if (this.authService.isUserLoggedIn()) {
      this.activeFeedType.set('PERSONALIZED');
      this.loadInitialFeed();
      this.loadMyCommunities(); // ✅ Load user's communities
    } else {
      this.activeFeedType.set('POPULAR');
      this.loadPopularFeed();
    }
    // Load content for all users
    this.loadTopUniversities();
    this.loadLeaderboard(); // ✅ Load leaderboard
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
  
  loadPopularFeed(): void {
    this.isLoading.set(true);
    this.hasMoreItems.set(true);
    this.currentPage = 0;
    this.feedService.getPopularFeed(this.currentPage).subscribe(page => {
      this.feedItems.set(page.content);
      this.hasMoreItems.set(!page.last);
      this.isLoading.set(false);
    });
  }
  
  loadMore(): void {
    if (this.isLoadingMore() || !this.hasMoreItems()) return;

    this.isLoadingMore.set(true);
    this.currentPage++;

    const loadMore$ = this.activeFeedType() === 'PERSONALIZED' 
      ? this.feedService.getFeed(this.currentPage)
      : this.feedService.getPopularFeed(this.currentPage);

    loadMore$.subscribe(page => {
      this.feedItems.update(currentItems => [...currentItems, ...page.content]);
      this.hasMoreItems.set(!page.last);
      this.isLoadingMore.set(false);
    });
  }

  loadTopUniversities(): void {
    this.universityService.getTopUniversities().subscribe(universities => {
      this.topUniversities.set(universities);
    });
  }

  // ✅ NEW Method to load user's communities
  loadMyCommunities(): void {
    this.userService.getUserCommunities().subscribe(communities => {
      this.myCommunities.set(communities);
    });
  }

  // ✅ NEW Method to load the leaderboard
  loadLeaderboard(): void {
    this.userService.getLeaderboard().subscribe(users => {
      this.leaderboard.set(users);
    });
  }

  handleAskQuestionClick(modalFooter: TemplateRef<{}>): void {
    if (this.authService.isUserLoggedIn()) {
      this.router.navigate(['/qa/ask']);
    } else {
      this.activeModal = this.modal.create({
        nzTitle: 'Login Required',
        nzContent: 'You must be logged in to ask a question. Please log in or create a free account.',
        nzFooter: modalFooter,
        nzMaskClosable: true,
        nzClosable: true
      });

      this.activeModal.afterClose.subscribe(() => {
        this.activeModal = null;
      });
    }
  }

  handleCancel(): void {
    this.activeModal?.destroy();
  }

  handleRegister(): void {
    this.activeModal?.destroy();
    this.router.navigate(['/auth/register']);
  }

  handleLogin(): void {
    this.activeModal?.destroy();
    this.router.navigate(['/auth/login']);
  }
}