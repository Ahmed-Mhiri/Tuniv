import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { of, switchMap } from 'rxjs';

// Models
import { Question } from '../../../../shared/models/qa.model'; // <-- TYPE CHANGED

// Services
import { QuestionService } from '../../services/question.service';
import { AuthService } from '../../../../core/services/auth.service';

// Shared Components & Pipes
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';
import { AnswerItemComponent } from '../../components/answer-item/answer-item.component';
import { VoteComponent } from '../../../../shared/components/vote/vote.component';

// NG-ZORRO
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzAlertComponent, NzAlertModule } from 'ng-zorro-antd/alert';
import { AnswerFormComponent, AnswerSubmitEvent } from '../../components/answer-form/answer-form';
import { NzMessageService } from 'ng-zorro-antd/message';
import { VoteService } from '../../services/vote.service';

@Component({
  selector: 'app-question-detail-page',
  standalone: true,
  imports: [
    RouterLink,
    SpinnerComponent,
    TimeAgoPipe,
    AnswerItemComponent,
    VoteComponent,
    NzAvatarModule,
    NzDividerModule,
    NzEmptyModule,
    NzAlertComponent,
    NzAlertModule, // <-- Import NzAlertModule
    AnswerFormComponent, // <-- Add the new form component
  ],
  templateUrl: './question-detail-page.component.html',
  styleUrl: './question-detail-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionDetailPageComponent implements OnInit {
  // --- Injections ---
  private readonly route = inject(ActivatedRoute);
  private readonly questionService = inject(QuestionService);
  public readonly authService = inject(AuthService);
  private readonly message = inject(NzMessageService);
  private readonly voteService = inject(VoteService);

  // --- State ---
  readonly isLoading = signal(true);
  readonly isSubmittingAnswer = signal(false);
  readonly question = signal<Question | null>(null);

  // --- Computed State ---
  readonly isCurrentUserQuestionAuthor = computed(() => {
    const questionAuthorId = this.question()?.author?.userId;
    const currentUserId = this.authService.currentUser()?.userId;
    return !!(questionAuthorId && currentUserId && questionAuthorId === currentUserId);
  });

  ngOnInit(): void {
  this.route.paramMap.pipe(
    switchMap(params => {
      this.isLoading.set(true);
      const questionId = Number(params.get('id'));

      if (isNaN(questionId)) {
        this.question.set(null);
        this.isLoading.set(false);
        return of(null);
      }
      // This is the GET request that is failing silently
      return this.questionService.getQuestionById(questionId);
    })
  ).subscribe({
    next: (data) => {
      // This runs if the GET request succeeds
      this.question.set(data);
      this.isLoading.set(false);
    },
    error: (err) => {
      // ✅ THIS BLOCK WILL SHOW YOU THE REAL PROBLEM
      // This runs when the GET request fails
      console.error('CRITICAL FRONTEND ERROR: Failed to load question data:', err);
      this.question.set(null);
      this.isLoading.set(false);
    }
  });
}
  
  refreshData(): void {
  const questionId = this.question()?.questionId;
  if (questionId) {
    // This delay is critical to prevent the race condition.
    setTimeout(() => {
      this.questionService.getQuestionById(questionId).subscribe({
        next: (data) => {
          this.question.set(data);
        },
        error: (err) => {
          console.error('Failed to refresh question data:', err);
        }
      });
    }, 300);
  }
}


  handleAnswerSubmit(event: AnswerSubmitEvent): void {
    const questionId = this.question()?.questionId;
    if (!questionId) return;

    this.isSubmittingAnswer.set(true);
    this.questionService.addAnswer(questionId, { body: event.body }, event.files).subscribe({

      next: () => {
        this.message.success('Your answer has been posted!');
        // --- FIX: This call ensures the page updates with the new answer ---
        this.refreshData();
      },
      error: () => {
        this.message.error('Failed to post your answer. Please try again.');
      },
      complete: () => {
        this.isSubmittingAnswer.set(false);
      }
    });
  }

  handleQuestionVote(voteValue: 1 | -1): void {
    const questionId = this.question()?.questionId;
    if (!questionId) return;
    this.voteService.voteOnQuestion(questionId, voteValue).subscribe({
      next: () => {
        this.message.success('Vote registered!');
        this.refreshData();
      },
      error: (err) => {
        this.message.error(err.error?.message || 'Failed to register vote.');
      }
    });
  }
}