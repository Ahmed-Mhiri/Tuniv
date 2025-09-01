import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  debounceTime,
  distinctUntilChanged,
  filter,
  switchMap,
  catchError,
  tap,
  takeUntil
} from 'rxjs/operators';
import { of, Subject } from 'rxjs';
import { Question } from '../../models/qa.model';
import { QuestionService } from '../../../features/qa/services/question.service';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSpinComponent } from 'ng-zorro-antd/spin';

@Component({
  selector: 'app-search-bar',
  imports: [NzInputModule,NzSpinComponent,ReactiveFormsModule],
  templateUrl: './search-bar.html',
  styleUrls: ['./search-bar.scss']
})
export class SearchBarComponent implements OnInit, OnDestroy {
  private readonly questionService = inject(QuestionService);
  private readonly router = inject(Router);

  // A form control to manage the input field's state and value
  searchControl = new FormControl('');
  
  // State for the template
  searchResults: Question[] = [];
  isLoading = false;
  showResults = false;
  
  // Subject to handle component destruction for unsubscribing
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.searchControl.valueChanges
      .pipe(
        // Wait for 400ms after the user stops typing
        debounceTime(400),
        // Only proceed if the new value is different from the previous one
        distinctUntilChanged(),
        // Show loading spinner
        tap(() => {
          this.isLoading = true;
          this.showResults = true; // Show the dropdown as soon as they type
        }),
        // Use switchMap to cancel previous pending requests
        switchMap(query => {
          if (!query || query.trim().length < 2) {
            this.isLoading = false;
            return of({ content: [] }); // Return empty if query is too short
          }
          return this.questionService.searchQuestions(query).pipe(
            catchError(() => {
              // Handle errors gracefully, e.g., log them
              console.error('Search failed');
              return of({ content: [] }); // Return empty on error
            })
          );
        }),
        // Unsubscribe when the component is destroyed
        takeUntil(this.destroy$)
      )
      .subscribe(response => {
        this.isLoading = false;
        this.searchResults = response.content;
      });
  }

  // Navigate to the full question page when a result is clicked
  selectQuestion(questionId: number): void {
    this.showResults = false;
    this.searchControl.setValue('', { emitEvent: false }); // Clear input without triggering a new search
    this.searchResults = [];
    this.router.navigate(['/qa/questions', questionId]);
  }

  // Method to close dropdown when clicking outside
  closeResults(): void {
    // A small delay allows the click on a result to register before closing
    setTimeout(() => {
      this.showResults = false;
    }, 100);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}