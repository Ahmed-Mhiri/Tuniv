export interface Pageable {
  page: number;
  size: number;
  sort?: string; // Sort is often optional
}

/**
 * A generic interface for a paginated API RESPONSE from the backend.
 * ✅ CORRECTED: This now perfectly matches the default Spring Boot Page JSON structure.
 * @template T The type of the items in the content array.
 */
export interface Page<T> {
  content: T[];
  
  // Page metadata from Spring Boot
  last: boolean;
  totalPages: number;
  totalElements: number;
  size: number;         // Corresponds to pageSize
  number: number;       // Corresponds to pageNumber (zero-based index)
  
  // Additional useful properties from Spring Boot
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
  first: boolean;
  numberOfElements: number;
  empty: boolean;
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