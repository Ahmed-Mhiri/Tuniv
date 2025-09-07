import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UniversityService } from '../../services/university.service';
import { University } from '../../../../shared/models/university.model';
import { Page, PageInfo } from '../../../../shared/models/pagination.model';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';
import { PaginatorComponent } from '../../../../shared/components/paginator/paginator.component';
import { AuthService } from '../../../../core/services/auth.service';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

// NG-ZORRO Imports
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzBadgeModule } from 'ng-zorro-antd/badge';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { FormsModule } from '@angular/forms';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-university-list-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    SpinnerComponent,
    PaginatorComponent,
    NzCardModule,
    NzAlertModule,
    NzTypographyModule,
    NzButtonModule,
    NzPopconfirmModule,
    NzBadgeModule,
    NzIconModule,
    NzInputModule,
    NzEmptyModule,
  ],
  templateUrl: './university-list-page.component.html',
  styleUrl: './university-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UniversityListPageComponent implements OnInit, OnDestroy {
  // --- Dependencies ---
  private readonly universityService = inject(UniversityService);
  private readonly message = inject(NzMessageService);
  public readonly authService = inject(AuthService);

  // --- State Signals ---
  private readonly pagedResult = signal<any | null>(null); // Use 'any' to accommodate the nested structure for now
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionInProgress = signal<number | null>(null);

  // --- Pagination & Search State ---
  private readonly page = signal(0);
  private readonly size = signal(12); // Reverted back to 12
  public readonly searchTerm = signal('');

  // --- RxJS for Debounced Search ---
  public readonly searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;

  // --- Computed Signals for the Template ---
  readonly universities = computed(() => this.pagedResult()?.content ?? []);

  readonly pageInfo = computed<PageInfo | null>(() => {
    const pagedData = this.pagedResult();

    // Check if data and the nested 'page' object exist
    if (!pagedData || !pagedData.page) {
      return null;
    }

    // ✅ CORRECTED: Read from the nested 'page' object from the backend
    return {
      pageNumber: pagedData.page.number,
      pageSize: pagedData.page.size,
      totalElements: pagedData.page.totalElements,
    };
  });

  ngOnInit(): void {
    this.fetchUniversities();

    this.searchSubscription = this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((term) => {
        this.searchTerm.set(term);
        this.page.set(0);
        this.fetchUniversities();
      });
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  // --- Data Fetching ---
  private fetchUniversities(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.universityService
      .getAllUniversities(this.page(), this.size(), this.searchTerm())
      .subscribe({
        next: (data) => {
          this.pagedResult.set(data);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('API Error:', err);
          this.error.set('Failed to load universities. Please try again later.');
          this.isLoading.set(false);
        },
      });
  }

  // --- Event Handlers ---
  onPageChange(newPage: number): void {
    this.page.set(newPage);
    this.fetchUniversities();
  }

  onPageSizeChange(newSize: number): void {
    this.size.set(newSize);
    this.page.set(0);
    this.fetchUniversities();
  }

  join(university: University): void {
    if (!this.authService.isUserLoggedIn()) {
      this.message.info('You need to be logged in to join a university.');
      return;
    }
    this.actionInProgress.set(university.universityId);
    this.universityService.joinUniversity(university.universityId).subscribe({
      next: () => {
        this.message.success(`Successfully joined ${university.name}`);
        this.fetchUniversities();
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to join.'),
      complete: () => this.actionInProgress.set(null),
    });
  }

  unjoin(university: University): void {
    this.actionInProgress.set(university.universityId);
    this.universityService.unjoinUniversity(university.universityId).subscribe({
      next: () => {
        this.message.success(`Successfully unjoined ${university.name}`);
        this.fetchUniversities();
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to unjoin.'),
      complete: () => this.actionInProgress.set(null),
    });
  }
}