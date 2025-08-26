import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FileUploadResponse } from '../../shared/models/file.model';

@Injectable({
  providedIn: 'root'
})
export class FileUploadService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * Uploads a file to the backend.
   * @param file The file object to upload.
   * @returns An Observable with the URL of the uploaded file.
   */
  uploadFile(file: File): Observable<FileUploadResponse> {
    // FormData is the standard way to send files via HTTP.
    const formData = new FormData();
    
    // The key 'file' must match the @RequestParam("file") on your backend controller.
    formData.append('file', file, file.name);

    return this.http.post<FileUploadResponse>(`${this.apiUrl}/files/upload`, formData);
  }
}