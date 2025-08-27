import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UniversityService } from '../../services/university.service';
import { University } from '../../../../shared/models/university.model';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';

// NG-ZORRO Imports
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzBadgeModule } from 'ng-zorro-antd/badge';
import { NzIconModule } from 'ng-zorro-antd/icon';

@Component({
  selector: 'app-university-list-page',
  imports: [
    RouterLink,
    SpinnerComponent,
    NzCardModule,
    NzAlertModule,
    NzTypographyModule,
    NzButtonModule,
    NzPopconfirmModule,
    NzBadgeModule,
    NzIconModule,
  ],
  templateUrl: './university-list-page.component.html',
  styleUrl: './university-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UniversityListPageComponent implements OnInit {
  // --- Dependencies ---
  private readonly universityService = inject(UniversityService);
  private readonly message = inject(NzMessageService);

  // --- State Signals ---
  readonly universities = signal<University[]>([]);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);
  
  // A signal to track which specific button is loading
  readonly actionInProgress = signal<number | null>(null);

  ngOnInit(): void {
    this.fetchUniversities();
  }

  // --- REFACTORED: Central method to fetch data ---
  private fetchUniversities(): void {
    this.isLoading.set(true);
    this.universityService.getAllUniversities().subscribe({
      next: (data) => {
        this.universities.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Failed to load universities. Please try again later.');
        this.isLoading.set(false);
      },
    });
  }

  // --- REFACTORED: Re-fetches data on success ---
  join(university: University): void {
    this.actionInProgress.set(university.universityId);
    this.universityService.joinUniversity(university.universityId).subscribe({
      next: () => {
        this.message.success(`Successfully joined ${university.name}`);
        this.fetchUniversities(); // Re-fetch the list to get the true state
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to join.'),
      complete: () => this.actionInProgress.set(null),
    });
  }

  // --- REFACTORED: Re-fetches data on success ---
  unjoin(university: University): void {
    this.actionInProgress.set(university.universityId);
    this.universityService.unjoinUniversity(university.universityId).subscribe({
      next: () => {
        this.message.success(`Successfully unjoined ${university.name}`);
        this.fetchUniversities(); // Re-fetch the list to get the true state
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to unjoin.'),
      complete: () => this.actionInProgress.set(null),
    });
  }
}