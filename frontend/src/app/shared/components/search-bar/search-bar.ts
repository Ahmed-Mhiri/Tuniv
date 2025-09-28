import { Component, DestroyRef, inject, input, OnDestroy, OnInit, signal } from '@angular/core';
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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-search-bar',
  imports: [NzInputModule,NzSpinComponent,ReactiveFormsModule],
  templateUrl: './search-bar.html',
  styleUrls: ['./search-bar.scss']
})
export class SearchBarComponent implements OnInit {
  // --- Dependencies ---
  private readonly questionService = inject(QuestionService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // ✅ NEW: A reusable input for the placeholder text.
  placeholder = input<string>('Search for questions...');

  // --- State ---
  searchControl = new FormControl('');

  // ✅ REFACTORED: Use signals for reactive state management.
  searchResults = signal<Question[]>([]);
  isLoading = signal(false);
  showResults = signal(false);

  ngOnInit(): void {
    this.searchControl.valueChanges
      .pipe(
        debounceTime(400),
        distinctUntilChanged(),
        tap(() => {
          // Use .set() to update signal values
          this.isLoading.set(true);
          this.showResults.set(true);
        }),
        switchMap(query => {
          if (!query || query.trim().length < 2) {
            return of({ content: [] }); // Return an empty observable
          }
          return this.questionService.searchQuestions(query).pipe(
            catchError(() => {
              console.error('Search failed');
              return of({ content: [] }); // Gracefully handle errors
            })
          );
        }),
        // ✅ REFACTORED: Cleaner way to handle unsubscription.
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(response => {
        this.isLoading.set(false);
        this.searchResults.set(response.content);
      });
  }

  selectQuestion(questionId: number): void {
    this.showResults.set(false);
    this.searchControl.setValue('', { emitEvent: false });
    this.searchResults.set([]);
    this.router.navigate(['/qa/questions', questionId]);
  }

  closeResults(): void {
    // A small delay allows the `mousedown` event on a result to fire first.
    setTimeout(() => {
      this.showResults.set(false);
    }, 150);
  }
}
