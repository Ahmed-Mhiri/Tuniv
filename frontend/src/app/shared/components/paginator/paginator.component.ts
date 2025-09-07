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
  data = input.required<Page<any> | PageInfo | null>({ alias: 'page' }); 
  pageSizeOptions = input<number[]>([10, 20, 50, 100]);

  // --- OUTPUTS ---
  pageChange = output<number>();
  pageSizeChange = output<number>();

  // --- COMPUTED SIGNALS ---

  // Safely extracts PageInfo from either a full Page object or a PageInfo object
  readonly pageInfo = computed<PageInfo | null>(() => {
    const pageData = this.data();
    if (!pageData) {
      return null;
    }

    // Check if it's a full Page<T> object from the API
    if ('content' in pageData) {
      return {
        // ✅ CORRECTED: Read from the new flattened structure
        pageNumber: pageData.pageNumber,
        pageSize: pageData.pageSize,
        totalElements: pageData.totalElements,
      };
    }
    // It's already a PageInfo object
    return pageData;
  });

  // Determines if the paginator should be visible
  readonly shouldShowPaginator = computed(() => {
    const info = this.pageInfo();
    // Show if there is more than one page
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