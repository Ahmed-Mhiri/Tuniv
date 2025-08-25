// This interface defines the shape of the pagination data from our backend.
export interface PageInfo {
  pageNumber: number;   // The current page index (zero-based)
  pageSize: number;     // The number of items per page
  totalElements: number; // The total number of items across all pages
}

// A generic interface for a Page object from the backend
export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  // ... and any other fields your backend's Page object has
}