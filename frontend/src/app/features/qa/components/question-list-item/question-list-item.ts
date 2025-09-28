import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Question, QuestionSummaryDto } from '../../../../shared/models/qa.model';
import { RouterModule } from '@angular/router';
import { TimeAgoPipe } from "../../../../shared/pipes/time-ago.pipe";
import { CommonModule } from '@angular/common';
import { QuestionService } from '../../services/question.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-question-list-item',
  imports: [RouterModule, TimeAgoPipe,CommonModule],
  templateUrl: './question-list-item.html',
  styleUrl: './question-list-item.scss'
})
export class QuestionListItemComponent {
  // --- Inputs ---
  // The required question data from the parent
  question = input.required<QuestionSummaryDto>();
  // ✅ NEW: Controls visibility of the university link
  showUniversity = input<boolean>(false);
  // ✅ NEW: Controls visibility and functionality of vote buttons
  interactive = input<boolean>(true);

  // --- Services ---
  private questionService = inject(QuestionService);
  private authService = inject(AuthService);

  // ✅ NEW: Local, writable signal to manage state for optimistic updates
  // This allows us to instantly reflect votes in the UI.
  public questionState = signal<QuestionSummaryDto>({} as QuestionSummaryDto);

  constructor() {
    // Keep the local state in sync with the input property
    effect(() => {
      this.questionState.set({ ...this.question() });
    });
  }

  // --- Computed Signals ---
  // Determines the CSS class for the answer count based on its value
  answerStatus = computed(() => {
    const q = this.questionState();
    if (q.hasAcceptedAnswer) {
      return 'has-accepted-answer';
    }
    if (q.answerCount > 0) {
      return 'has-answers';
    }
    return 'no-answers';
  });

  // --- Methods ---
  /** ✅ NEW: Handles the upvote action */
  vote(newVote: 'UPVOTE' | 'DOWNVOTE'): void {
    if (!this.authService.isUserLoggedIn()) {
      console.log('User must be logged in to vote.');
      return;
    }

    const currentState = this.questionState();
    const currentVote = currentState.userVoteStatus;
    let scoreModifier = 0;
    let finalVote: 'UPVOTE' | 'DOWNVOTE' | null;

    // This optimistic UI logic remains the same
    if (newVote === currentVote) {
      scoreModifier = newVote === 'UPVOTE' ? -1 : 1;
      finalVote = null;
    } else {
      scoreModifier = newVote === 'UPVOTE' ? 1 : -1;
      if (currentVote !== null) {
        scoreModifier *= 2;
      }
      finalVote = newVote;
    }

    this.questionState.update(q => ({
      ...q,
      score: q.score + scoreModifier,
      userVoteStatus: finalVote,
    }));

    // ✅ FIX: The service call logic is now updated
    let voteRequest$: Observable<Question>;

    if (finalVote !== null) {
      // Case 1: Cast or change a vote
      const voteValue = finalVote === 'UPVOTE' ? 1 : -1; // Translate string to number
      voteRequest$ = this.questionService.voteOnQuestion(currentState.id, voteValue);
    } else {
      // Case 2: Undo the vote
      voteRequest$ = this.questionService.removeVoteOnQuestion(currentState.id);
    }

    voteRequest$.subscribe({
      error: () => {
        console.error('Failed to process vote.');
        this.questionState.set(currentState); // Revert UI on error
      },
    });
  }
}