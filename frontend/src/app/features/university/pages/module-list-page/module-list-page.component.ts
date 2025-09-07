import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BehaviorSubject, combineLatest, switchMap } from 'rxjs';
import { UniversityService } from '../../services/university.service';
import { Module } from '../../../../shared/models/university.model';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';

// NG-ZORRO Imports
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { ModuleService } from '../../services/module.service';
import { Pageable, PageInfo } from '../../../../shared/models/pagination.model';
import { NzPaginationComponent } from 'ng-zorro-antd/pagination';

@Component({
  selector: 'app-module-list-page',
  imports: [
    RouterLink,
    SpinnerComponent,
    NzCardModule,
    NzAlertModule,
    NzTypographyModule,
    NzIconModule,
    NzButtonModule,
    NzPaginationComponent
  ],
  templateUrl: './module-list-page.component.html',
  styleUrl: './module-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModuleListPageComponent implements OnInit {
  private readonly moduleService = inject(ModuleService);
  private readonly route = inject(ActivatedRoute);

  // --- State Signals ---
  readonly modules = signal<Module[]>([]);
  readonly universityName = signal<string>('');
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);
  
  // New state for pagination
  readonly pageInfo = signal<PageInfo>({
    pageNumber: 0,
    pageSize: 12, // Set a default page size
    totalElements: 0,
  });

  // Use a BehaviorSubject to trigger data refetching when page changes
  private readonly pageable$ = new BehaviorSubject<Pageable>({
    page: this.pageInfo().pageNumber,
    size: this.pageInfo().pageSize,
  });

  ngOnInit(): void {
    // Combine paramMap with our pageable$ stream
    combineLatest([this.route.paramMap, this.pageable$])
      .pipe(
        switchMap(([params, pageable]) => {
          const universityId = Number(params.get('id'));
          this.isLoading.set(true);
          return this.moduleService.getModulesByUniversity(universityId, pageable);
        })
      )
      .subscribe({
        next: (page) => {
          this.modules.set(page.content);
          // Update pageInfo signal with the response metadata
          this.pageInfo.set({
            pageNumber: page.pageNumber,
            pageSize: page.pageSize,
            totalElements: page.totalElements,
          });
          this.isLoading.set(false);
        },
        error: () => {
          this.error.set('Failed to load modules. Please try again later.');
          this.isLoading.set(false);
        },
      });
  }

  // Method to handle page changes from the pagination component
  onPageChange(pageIndex: number): void {
    // Ng-Zorro pagination is 1-based, Spring is 0-based.
    const zeroBasedIndex = pageIndex - 1;
    this.pageable$.next({
      page: zeroBasedIndex,
      size: this.pageInfo().pageSize,
    });
  }
}