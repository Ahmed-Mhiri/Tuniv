import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from "./shared/components/navbar/navbar.component";
import { FooterComponent } from "./shared/components/footer/footer.component";
import { VoteComponent } from "./shared/components/vote/vote.component";
import { SpinnerComponent } from './shared/components/spinner/spinner.component';
import { Page } from './shared/models/pagination.model';
import { PaginatorComponent } from "./shared/components/paginator/paginator.component";
import { JsonPipe } from '@angular/common';
import { FileUploadComponent } from "./shared/components/file-upload/file-upload.component";

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavbarComponent, FooterComponent, VoteComponent, SpinnerComponent, PaginatorComponent, JsonPipe, FileUploadComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('frontend');
  isLoading = false;

  uploadedImageFiles = signal<File[]>([]);

  // Signal to hold the file from the custom PDF uploader
  uploadedPdfFile = signal<File | null>(null);

  // Handler for the default image uploader
  onImagesChanged(files: File[]): void {
    console.log('Received image files:', files);
    this.uploadedImageFiles.set(files);
  }

  // Handler for the custom PDF uploader
  onPdfChanged(files: File[]): void {
    console.log('Received PDF file:', files[0]);
    this.uploadedPdfFile.set(files.length > 0 ? files[0] : null);
  }
}
