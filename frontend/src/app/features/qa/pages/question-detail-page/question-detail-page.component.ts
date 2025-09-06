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
import { AnswerFormComponent, AnswerSubmitEvent } from '../../components/answer-form/answer-form';
import { NzMessageService } from 'ng-zorro-antd/message';
import { VoteService } from '../../services/vote.service';
import { NzModalModule, NzModalService } from 'ng-zorro-antd/modal';
import { PostEditFormComponent, PostEditSaveEvent } from '../../components/post-edit-form.component/post-edit-form.component';
import { NzIconModule } from 'ng-zorro-antd/icon';

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
    PostEditFormComponent,
    NzModalModule,
    NzIconModule

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
  private readonly voteService = inject(VoteService);
  private readonly modal = inject(NzModalService);

  // --- State ---
  readonly isLoading = signal(true);
  readonly isSubmittingAnswer = signal(false);
  readonly question = signal<Question | null>(null);
  readonly isEditing = signal(false); // ✨ New state for editing

  // --- Computed State ---
  readonly isCurrentUserQuestionAuthor = computed(() => {
    const questionAuthorId = this.question()?.author?.userId;
    const currentUserId = this.authService.currentUser()?.userId;
    // Ensure both values are valid before comparing
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
          this.question.set(null);
          this.isLoading.set(false);
          return of(null);
        }
        return this.questionService.getQuestionById(questionId);
      })
    ).subscribe({
      next: (data) => {
        this.question.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load question data:', err);
        this.message.error('Could not load the question.');
        this.question.set(null);
        this.isLoading.set(false);
        this.router.navigate(['/']); // Navigate away if question not found
      }
    });
  }
  
  refreshData(): void {
    const questionId = this.question()?.questionId;
    if (!questionId) return;

    // The refresh is now immediate. We will control WHEN it's called.
    this.isLoading.set(true); // Give user feedback that data is refreshing
    this.questionService.getQuestionById(questionId).pipe(
      finalize(() => this.isLoading.set(false)) // Always stop loading indicator
    ).subscribe({
      next: (data) => this.question.set(data),
      error: (err) => this.message.error('Failed to refresh question data.'),
    });
  }

  // ✨ --- NEW: DELETE LOGIC --- ✨
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
            this.router.navigate(['/']); // Navigate to home page after deletion
          },
          error: (err) => this.message.error(err.error?.message || 'Failed to delete question.'),
        }),
    });
  }

  // ✨ --- NEW: UPDATE LOGIC --- ✨
  handleSave(event: PostEditSaveEvent): void {
    const questionId = this.question()?.questionId;
    if (!questionId || !event.title) return;

    const request: QuestionUpdateRequest = {
      title: event.title,
      body: event.body,
      attachmentIdsToDelete: event.attachmentIdsToDelete,
    };

    this.questionService.updateQuestion(questionId, request, event.newFiles).pipe(
      // Finalize ensures this runs after the update is complete (success or error)
      finalize(() => {
        this.isEditing.set(false);
        this.refreshData(); // Refresh AFTER the update is done
      })
    ).subscribe({
      next: () => this.message.success('Question updated successfully!'),
      error: (err) => this.message.error(err.error?.message || 'Failed to update question.'),
    });
  }

  handleAnswerSubmit(event: AnswerSubmitEvent): void {
    const questionId = this.question()?.questionId;
    if (!questionId) return;

    this.isSubmittingAnswer.set(true);
    this.questionService.addAnswer(questionId, { body: event.body }, event.files).pipe(
      // Refresh the entire page's data only after the answer is successfully posted.
      finalize(() => {
        this.isSubmittingAnswer.set(false);
        this.refreshData();
      })
    ).subscribe({
      next: () => this.message.success('Your answer has been posted!'),
      error: () => this.message.error('Failed to post your answer. Please try again.'),
    });
  }
  
  // The same applies to any action that modifies child data, like voting or adding comments
  // The child component should emit `(updateRequired)`, and the parent's `refreshData` will handle it.
  // The key is that `refreshData` itself is now immediate and reliable.

  handleQuestionVote(voteValue: 1 | -1): void {
    const questionId = this.question()?.questionId;
    if (!questionId) return;
    this.voteService.voteOnQuestion(questionId, voteValue).subscribe({
      next: () => {
        this.message.success('Vote registered!');
        this.refreshData();
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to register vote.'),
    });
  }
}