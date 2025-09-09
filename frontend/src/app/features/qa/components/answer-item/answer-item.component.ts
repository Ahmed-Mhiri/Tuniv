import { ChangeDetectionStrategy, ChangeDetectorRef, Component, computed, inject, input, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, of } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { Answer, CommentCreateRequest, Question } from '../../../../shared/models/qa.model';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';
import { VoteComponent } from '../../../../shared/components/vote/vote.component';
import { CommentItemComponent } from '../comment-item/comment-item.component';
import { CommentService } from '../../services/comment.service';

// NG-ZORRO Modules
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzUploadChangeParam, NzUploadFile, NzUploadModule } from 'ng-zorro-antd/upload';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { NzModalService } from 'ng-zorro-antd/modal';
import { PostEditFormComponent, PostEditSaveEvent } from '../post-edit-form.component/post-edit-form.component';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-answer-item',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, TimeAgoPipe, VoteComponent, CommentItemComponent,
    NzAvatarModule, NzButtonModule, NzFormModule, NzInputModule, NzIconModule,PostEditFormComponent,
    NzUploadModule, NzToolTipModule, NzSpaceModule
  ],
  templateUrl: './answer-item.component.html',
  styleUrl: './answer-item.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnswerItemComponent {
  // --- Injections ---
  private readonly fb = inject(FormBuilder);
  private readonly commentService = inject(CommentService);
  private readonly message = inject(NzMessageService);
  public readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly modal = inject(NzModalService);
  
  // ✅ REMOVED AnswerService and VoteService, ADDED QuestionService
  private readonly questionService = inject(QuestionService);

  // --- Inputs & Outputs ---
  answer = input.required<Answer>();
  isQuestionAuthor = input<boolean>(false);
  
  // ✅ NEW: The questionId is now required for all API calls.
  questionId = input.required<number>();
  
  updated = output<Question>();
  deleted = output<void>();

  // --- State ---
  readonly commentFileList = signal<NzUploadFile[]>([]);
  readonly commentFilesToUpload = signal<File[]>([]);
  isCommentFormVisible = signal(false);
  isEditing = signal(false);
  isCurrentUserAuthor = computed(() => 
    this.authService.currentUser()?.userId === this.answer().author.userId
  );

  // --- Forms & Validation ---
  private atLeastOneFieldValidator = (control: AbstractControl): ValidationErrors | null => {
    const body = control.get('body')?.value;
    const hasFiles = this.commentFileList().length > 0;
    return !body?.trim() && !hasFiles ? { atLeastOneRequired: true } : null;
  };
  commentForm = this.fb.group({ body: [''] }, { validators: this.atLeastOneFieldValidator });

  /**
   * Handles a vote on this answer.
   */
  handleVote(voteValue: 1 | -1): void {
    if (!this.authService.isUserLoggedIn()) {
      this.message.info('You must be logged in to vote.');
      return;
    }
    // ✅ UPDATED: Call QuestionService's voteOnAnswer method
    this.questionService.voteOnAnswer(this.questionId(), this.answer().answerId, voteValue).subscribe({
      next: (updatedQuestion) => this.updated.emit(updatedQuestion),
      error: (err) => this.message.error(err.error?.message || 'Failed to vote.'),
    });
  }

  /**
   * Marks this answer as the solution for the question.
   */
  markAsSolution(): void {
    // ✅ UPDATED: Call QuestionService's markAsSolution method
    this.questionService.markAsSolution(this.questionId(), this.answer().answerId).subscribe({
      next: (updatedQuestion) => this.updated.emit(updatedQuestion),
      error: (err) => this.message.error(err.error?.message || 'Action failed.'),
    });
  }

  /**
   * Submits a new top-level comment to this answer.
   */
  submitComment(): void {
    if (this.commentForm.invalid) {
      Object.values(this.commentForm.controls).forEach(control => {
        control.markAsDirty();
        control.updateValueAndValidity({ onlySelf: true });
      });
      return;
    }
    const request: CommentCreateRequest = {
  body: this.commentForm.value.body!,
  parentCommentId: null,
  answerId: this.answer().answerId, // ✅ ADD THIS LINE
};
    // ✅ UPDATED: Call CommentService with the required answerId parameter
    this.commentService.createComment(this.answer().answerId, request, this.commentFilesToUpload()).subscribe({
      next: (updatedQuestion) => {
        this.commentForm.reset();
        this.commentFileList.set([]);
        this.commentFilesToUpload.set([]);
        this.isCommentFormVisible.set(false);
        this.updated.emit(updatedQuestion);
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to post comment.'),
    });
  }

  /**
   * Deletes this answer.
   */
  deleteAnswer(): void {
    this.modal.confirm({
      nzTitle: 'Delete this answer?',
      nzContent: 'This action cannot be undone.',
      nzOkText: 'Delete',
      nzOkDanger: true,
      nzOnOk: () =>
        // ✅ UPDATED: Call QuestionService's deleteAnswer method
        this.questionService.deleteAnswer(this.questionId(), this.answer().answerId).subscribe({
          next: () => this.deleted.emit(),
          error: (err) => this.message.error(err.error?.message || 'Failed to delete answer.'),
        }),
    });
  }
  
  /**
   * Saves edits made to this answer.
   */
  handleSave(event: PostEditSaveEvent): void {
    const request = {
      body: event.body,
      attachmentIdsToDelete: event.attachmentIdsToDelete,
    };
    // ✅ UPDATED: Call QuestionService's updateAnswer method
    this.questionService.updateAnswer(this.questionId(), this.answer().answerId, request, event.newFiles).subscribe({
      next: (updatedQuestion) => {
        this.isEditing.set(false);
        this.updated.emit(updatedQuestion);
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to update answer.'),
    });
  }

  // --- File Handling Methods (Unchanged) ---
  beforeUpload = (file: NzUploadFile): Observable<boolean> => {
    this.commentFilesToUpload.update(list => [...list, file as unknown as File]);
    this.commentFileList.update(list => [...list, file]);
    this.commentForm.updateValueAndValidity(); // Ensure form validation runs
    this.cdr.detectChanges();
    return of(false);
  };

  handleCommentFileChange(info: NzUploadChangeParam): void {
    if (info.type === 'removed') {
      this.removeCommentFile(info.file);
    }
  }

  removeCommentFile(fileToRemove: NzUploadFile): void {
    this.commentFileList.update(list => list.filter(f => f.uid !== fileToRemove.uid));
    this.commentFilesToUpload.update(list => (list as unknown as NzUploadFile[]).filter(f => f.uid !== fileToRemove.uid) as unknown as File[]);
    this.commentForm.updateValueAndValidity(); // Ensure form validation runs
    this.cdr.detectChanges();
  }
}