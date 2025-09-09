import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, of, switchMap } from 'rxjs';

// Models
import { Question, QuestionUpdateRequest } from '../../../../shared/models/qa.model'; // <-- TYPE CHANGED

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
import { AnswerForm, AnswerSubmitEvent } from '../../components/answer-form/answer-form';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule, NzModalService } from 'ng-zorro-antd/modal';
import { PostEditFormComponent, PostEditSaveEvent } from '../../components/post-edit-form.component/post-edit-form.component';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { SafeHtmlPipe } from '../../../../shared/pipes/safe-html.pipe';

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
    AnswerForm, // <-- Add the new form component
    PostEditFormComponent,
    NzModalModule,
    NzIconModule,
    SpinnerComponent,
    SafeHtmlPipe

  ],
  templateUrl: './question-detail-page.component.html',
  styleUrl: './question-detail-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionDetailPageComponent implements OnInit {
  // --- Injections ---
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly questionService = inject(QuestionService);
  public readonly authService = inject(AuthService);
  private readonly message = inject(NzMessageService);
  private readonly modal = inject(NzModalService);

  // --- State ---
  readonly isLoading = signal(true);
  readonly isSubmittingAnswer = signal(false);
  readonly question = signal<Question | null>(null);
  readonly isEditing = signal(false);

  // --- Computed State ---
  readonly isCurrentUserQuestionAuthor = computed(() => {
    const questionAuthorId = this.question()?.author?.userId;
    const currentUserId = this.authService.currentUser()?.userId;
    return !!(questionAuthorId && currentUserId && questionAuthorId === currentUserId);
  });

  ngOnInit(): void {
    this.loadQuestionData();
  }

  private loadQuestionData(): void {
    this.route.paramMap.pipe(
      switchMap(params => {
        this.isLoading.set(true);
        const questionId = Number(params.get('id'));
        if (isNaN(questionId)) {
          return of(null);
        }
        return this.questionService.getQuestionById(questionId).pipe(
          finalize(() => this.isLoading.set(false))
        );
      })
    ).subscribe({
      next: (data) => {
        this.question.set(data);
      },
      error: (err) => {
        console.error('Failed to load question data:', err);
        this.message.error('Could not load the question.');
        this.router.navigate(['/']);
      }
    });
  }

  handleChildUpdate(updatedQuestion: Question): void {
    this.question.set(updatedQuestion);
    this.message.success('Update successful!');
  }

  handleChildDeletion(): void {
    this.message.success('Item deleted successfully.');
    this.loadQuestionData();
  }

  deleteQuestion(): void {
    const questionId = this.question()?.questionId;
    if (!questionId) return;

    this.modal.confirm({
      nzTitle: 'Delete this question?',
      nzContent: 'All of its answers and comments will also be permanently deleted. This action cannot be undone.',
      nzOkText: 'Yes, Delete Question',
      nzOkDanger: true,
      nzOnOk: () =>
        this.questionService.deleteQuestion(questionId).subscribe({
          next: () => {
            this.message.success('Question deleted successfully.');
            this.router.navigate(['/']);
          },
          error: (err) => this.message.error(err.error?.message || 'Failed to delete question.'),
        }),
    });
  }

  handleSave(event: PostEditSaveEvent): void {
    const questionId = this.question()?.questionId;
    if (!questionId || !event.title) return;

    const request: QuestionUpdateRequest = {
      title: event.title,
      body: event.body,
      attachmentIdsToDelete: event.attachmentIdsToDelete,
    };

    this.questionService.updateQuestion(questionId, request, event.newFiles).subscribe({
      next: (updatedQuestion) => {
        this.question.set(updatedQuestion);
        this.isEditing.set(false);
        this.message.success('Question updated successfully!');
      },
      error: (err) => {
        this.isEditing.set(false);
        this.message.error(err.error?.message || 'Failed to update question.');
      },
    });
  }

  handleAnswerSubmit(event: AnswerSubmitEvent): void {
    const questionId = this.question()?.questionId;
    if (!questionId) return;

    this.isSubmittingAnswer.set(true);
    this.questionService.addAnswer(questionId, { body: event.body }, event.files).pipe(
      finalize(() => this.isSubmittingAnswer.set(false))
    ).subscribe({
      next: (updatedQuestion) => {
        this.question.set(updatedQuestion);
        this.message.success('Your answer has been posted!');
      },
      error: () => this.message.error('Failed to post your answer. Please try again.'),
    });
  }

  handleQuestionVote(voteValue: 1 | -1): void {
    if (!this.authService.isUserLoggedIn()) {
      this.message.info('You must be logged in to vote.');
      return;
    }
    const questionId = this.question()?.questionId;
    if (!questionId) return;

    this.questionService.voteOnQuestion(questionId, voteValue).subscribe({
      next: (updatedQuestion) => {
        this.question.set(updatedQuestion);
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to register vote.'),
    });
  }
}