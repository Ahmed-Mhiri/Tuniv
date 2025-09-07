// src/app/shared/models/pagination.model.ts

/**
 * Interface for making paginated API REQUESTS.
 * Tells the backend which page and size to return.
 */
export interface Pageable {
  page: number;
  size: number;
  sort?: string; // Sort is often optional
}

/**
 * A generic interface for a paginated API RESPONSE from the backend.
 * Contains the actual data (`content`) and all page metadata.
 * @template T The type of the items in the content array.
 */
export interface Page<T> {
  content: T[];
  
  // ✅ CORRECTED: Properties are now at the top level
  pageNumber: number;
  pageSize: number;
  
  totalElements: number;
  totalPages: number;
  last: boolean;
}

/**
 * A simplified interface used as an @input for the PaginatorComponent.
 * It contains only the essential info the UI component needs to render.
 */
export interface PageInfo {
  pageNumber: number;   // The current page index (zero-based)
  pageSize: number;     // The number of items per page
  totalElements: number; // The total number of items across all pages
}