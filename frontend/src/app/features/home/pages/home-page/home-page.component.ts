import { AfterViewInit, Component, DestroyRef, ElementRef, OnInit, TemplateRef, ViewChild, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

// --- Services ---
import { FeedService } from '../../services/feed.service';
import { UniversityService } from '../../../university/services/university.service';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService } from '../../../user/services/user.service'; // ✅ IMPORT UserService

// --- Models ---
import { Question, QuestionSummaryDto } from '../../../../shared/models/qa.model';
import { University } from '../../../../shared/models/university.model';
import { LeaderboardUser, UserCommunity, UserProfile } from '../../../../shared/models/user.model'; // ✅ IMPORT new models

// --- Components ---
import { QuestionListItemComponent } from '../../../qa/components/question-list-item/question-list-item';
import { trigger, transition, style, animate } from '@angular/animations';


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
import { debounceTime, fromEvent } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzMenuModule } from 'ng-zorro-antd/menu';


@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [
    CommonModule, RouterLink, QuestionListItemComponent, NzGridModule,
    NzCardModule, NzButtonModule, NzIconModule, NzSpinModule,
    NzEmptyModule, NzDividerModule, NzListModule, NzModalModule, NzAvatarModule, NzTooltipModule, NzDropDownModule,NzMenuModule,
  ],
  templateUrl: './home-page.component.html',
  styleUrls: ['./home-page.component.scss'],
  animations: [
    // ✅ NEW: Animation for staggering feed items
    trigger('slideIn', [
      transition(':enter', [
        style({ transform: 'translateY(20px)', opacity: 0 }),
        animate('300ms ease-out', style({ transform: 'translateY(0)', opacity: 1 })),
      ]),
    ]),
  ],
})
export class HomePageComponent implements OnInit, AfterViewInit {

  readonly Math = Math;

  // --- Services & Dependencies ---
  private readonly feedService = inject(FeedService);
  private readonly universityService = inject(UniversityService);
  private readonly userService = inject(UserService);
  public readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly modal = inject(NzModalService);
  private readonly destroyRef = inject(DestroyRef);

  // --- Element Refs ---
  @ViewChild('loadMoreTrigger') loadMoreTrigger!: ElementRef;
  private intersectionObserver?: IntersectionObserver;

  // --- State Signals ---
  readonly feedItems = signal<QuestionSummaryDto[]>([]);
  readonly topUniversities = signal<University[]>([]);
  readonly myCommunities = signal<UserCommunity[]>([]);
  readonly leaderboard = signal<LeaderboardUser[]>([]);
  readonly currentUser = this.authService.currentUser;

  // --- UI/Loading State ---
  readonly isLoading = signal(true);
  readonly isLoadingMore = signal(false);
  readonly hasMoreItems = signal(true);

  // ✅ NEW: State for the mobile tab navigation
  readonly activeView = signal<'FEED' | 'COMMUNITIES' | 'LEADERBOARD'>('FEED');

  // ✅ NEW: State for hero card visibility
  readonly hasViewedHero = signal(false);

  // ✅ NEW: State for feed filtering
  readonly activeFeedType = signal<'PERSONALIZED' | 'POPULAR'>('POPULAR');
  readonly timeFilter = signal<'today' | 'week' | 'month' | 'all'>('all');

  // ✅ NEW: Signal to detect mobile viewport
  readonly isMobile = signal(window.innerWidth < 768);

  private currentPage = 0;
  private activeModal: NzModalRef | null = null;

  constructor() {
    // Re-evaluate feed type when auth state changes
    effect(() => {
      const isLoggedIn = this.authService.isUserLoggedIn();
      this.activeFeedType.set(isLoggedIn ? 'PERSONALIZED' : 'POPULAR');
      this.resetAndLoadFeed();
      if (isLoggedIn) {
        this.loadMyCommunities();
      }
    });

    // Listen to window resize to update isMobile signal
    fromEvent(window, 'resize')
      .pipe(debounceTime(100), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.isMobile.set(window.innerWidth < 768));
  }

  ngOnInit(): void {
    // Check session storage if hero has been dismissed
    if (sessionStorage.getItem('heroDismissed') === 'true') {
      this.hasViewedHero.set(true);
    }

    this.loadSidebarData();
  }

  ngAfterViewInit(): void {
    this.setupInfiniteScroll();
  }

  // --- Data Loading ---

  private loadFeed(loadMore = false): void {
  if (!loadMore) {
    this.isLoading.set(true);
    this.currentPage = 0;
    this.feedItems.set([]);
  }

  // ✅ NEW: Translate the string filter into a number for the service
  let timeFilterParam: number | undefined;
  switch (this.timeFilter()) {
    case 'today':
      timeFilterParam = 1;
      break;
    case 'week':
      timeFilterParam = 7;
      break;
    case 'month':
      timeFilterParam = 30;
      break;
    case 'all':
      timeFilterParam = undefined; // Send undefined for 'all time'
      break;
  }

  const feed$ =
    this.activeFeedType() === 'PERSONALIZED'
      // ✅ FIX: Pass the translated number parameter to the service
      ? this.feedService.getPersonalizedFeed(this.currentPage, timeFilterParam)
      : this.feedService.getPopularFeed(this.currentPage, timeFilterParam);

  feed$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
    next: page => {
      if (loadMore) {
        this.feedItems.update(current => [...current, ...page.content]);
      } else {
        this.feedItems.set(page.content);
      }
      this.hasMoreItems.set(!page.last);
      this.isLoading.set(false);
      this.isLoadingMore.set(false);
    },
    error: () => {
      this.isLoading.set(false);
      this.isLoadingMore.set(false);
    },
  });
}

  private loadSidebarData(): void {
    this.universityService.getTopUniversities().subscribe(unis => this.topUniversities.set(unis));
    this.userService.getLeaderboard().subscribe(users => this.leaderboard.set(users));
  }

  loadMyCommunities(): void {
  this.userService.getUserCommunities().subscribe(communities => {
    // ADD THIS LINE
    console.log('Communities data set in signal:', communities);

    this.myCommunities.set(communities);
  });
}

  // --- Event Handlers & UI Logic ---

  /** ✅ NEW: Switches between 'For You' and 'Trending' feeds */
  switchFeedType(type: 'PERSONALIZED' | 'POPULAR'): void {
    if (this.activeFeedType() === type) return;
    this.activeFeedType.set(type);
    this.resetAndLoadFeed();
    this.activeView.set('FEED'); // Switch back to feed view on mobile
  }

  /** ✅ NEW: Switches between different views on mobile */
  switchView(view: 'FEED' | 'COMMUNITIES' | 'LEADERBOARD'): void {
    this.activeView.set(view);
  }

  /** ✅ NEW: Sets the time filter and reloads the feed */
  setTimeFilter(filter: 'today' | 'week' | 'month' | 'all'): void {
    this.timeFilter.set(filter);
    this.resetAndLoadFeed();
  }

  /** ✅ NEW: Hides the hero card and remembers the choice for the session */
  dismissHero(): void {
    this.hasViewedHero.set(true);
    sessionStorage.setItem('heroDismissed', 'true');
  }

  /** Central method to reload feed from the start */
  private resetAndLoadFeed(): void {
    this.loadFeed(false);
  }

  /** Loads the next page of feed items */
  loadMore(): void {
    if (this.isLoadingMore() || !this.hasMoreItems()) return;
    this.isLoadingMore.set(true);
    this.currentPage++;
    this.loadFeed(true);
  }

  // --- Infinite Scroll Setup ---
  /** ✅ NEW: Sets up the Intersection Observer to trigger loadMore */
  private setupInfiniteScroll(): void {
    const options = { rootMargin: '0px 0px 400px 0px' };
    this.intersectionObserver = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        this.loadMore();
      }
    }, options);
    this.intersectionObserver.observe(this.loadMoreTrigger.nativeElement);
  }

  // --- Modal Logic (unchanged but included for completeness) ---

  handleAskQuestionClick(modalFooter: TemplateRef<{}>): void {
    if (this.authService.isUserLoggedIn()) {
      this.router.navigate(['/qa/ask']);
    } else {
      this.activeModal = this.modal.create({
        nzTitle: 'Join the Conversation',
        nzContent: 'You must be logged in to ask a question. Please sign in or create a free account to continue.',
        nzFooter: modalFooter,
        nzMaskClosable: true,
        nzClosable: true,
        nzCentered: true,
      });
      this.activeModal.afterClose.subscribe(() => (this.activeModal = null));
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