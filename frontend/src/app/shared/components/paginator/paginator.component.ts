// src/app/shared/components/paginator/paginator.component.ts

import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { NzPaginationModule } from 'ng-zorro-antd/pagination';
import { Page, PageInfo } from '../../../shared/models/pagination.model';

@Component({
  selector: 'app-paginator',
  standalone: true,
  imports: [NzPaginationModule],
  templateUrl: './paginator.component.html',
  styleUrl: './paginator.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaginatorComponent {
  // --- INPUTS ---
  // Allow binding the full Page object or just PageInfo for flexibility
  data = input.required<Page<any> | PageInfo>({ alias: 'page' }); 
  
  // New: Make the page size options configurable
  pageSizeOptions = input<number[]>([10, 20, 50, 100]);

  // --- OUTPUTS ---
  pageChange = output<number>();
  pageSizeChange = output<number>(); // New: Emit when page size changes

  // --- COMPUTED SIGNALS to centralize logic ---

  // Safely extracts PageInfo from either a full Page object or a PageInfo object
  readonly pageInfo = computed<PageInfo>(() => {
    const pageData = this.data();
    if ('content' in pageData) { // Check if it's a full Page<T> object
      return {
        pageNumber: pageData.pageable.pageNumber,
        pageSize: pageData.pageable.pageSize,
        totalElements: pageData.totalElements,
      };
    }
    return pageData; // It's already a PageInfo object
  });

  // Determines if the paginator should be visible
  readonly shouldShowPaginator = computed(() => {
    const info = this.pageInfo();
    return info && info.totalElements > info.pageSize;
  });

  // --- EVENT HANDLERS ---
  onPageIndexChange(newPageIndex: number): void {
    // Convert from nz-pagination's 1-based index to our 0-based index
    this.pageChange.emit(newPageIndex - 1);
  }
  
  onPageSizeChange(newPageSize: number): void {
    this.pageSizeChange.emit(newPageSize);
  }
}