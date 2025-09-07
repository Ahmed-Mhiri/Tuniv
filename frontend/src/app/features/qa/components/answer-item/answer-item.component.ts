import { ChangeDetectionStrategy, ChangeDetectorRef, Component, computed, inject, input, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, of } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { Answer, CommentCreateRequest } from '../../../../shared/models/qa.model';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';
import { VoteComponent } from '../../../../shared/components/vote/vote.component';
import { CommentItemComponent } from '../comment-item/comment-item.component';
import { AnswerService } from '../../services/answer.service';
import { VoteService } from '../../services/vote.service';
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
  private readonly fb = inject(FormBuilder);
  private readonly voteService = inject(VoteService);
  private readonly commentService = inject(CommentService);
  private readonly answerService = inject(AnswerService);
  private readonly message = inject(NzMessageService);
  public readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly modal = inject(NzModalService);
  answer = input.required<Answer>();
  isQuestionAuthor = input<boolean>(false);
  updateRequired = output<void>();
  readonly commentFileList = signal<NzUploadFile[]>([]);
  readonly commentFilesToUpload = signal<File[]>([]);
  isCommentFormVisible = signal(false);
  isEditing = signal(false);
  isCurrentUserAuthor = computed(() => 
    this.authService.currentUser()?.userId === this.answer().author.userId
  );

  private atLeastOneFieldValidator = (control: AbstractControl): ValidationErrors | null => {
    const body = control.get('body')?.value;
    const hasFiles = this.commentFileList().length > 0;
    return !body?.trim() && !hasFiles ? { atLeastOneRequired: true } : null;
  };
  commentForm = this.fb.group({
    body: [''],
  }, {
    validators: this.atLeastOneFieldValidator
  });

  beforeUpload = (file: NzUploadFile): Observable<boolean> => {
    this.commentFilesToUpload.update(list => [...list, file as unknown as File]);
    this.commentFileList.update(list => [...list, file]);
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
    this.cdr.detectChanges();
  }

  handleVote(voteValue: number): void {
    // ✅ ADD THIS CHECK
    if (!this.authService.isUserLoggedIn()) {
      this.message.info('You must be logged in to vote.');
      return;
    }
    
    const valueToSubmit = voteValue >= 0 ? 1 : -1;
    this.voteService.voteOnAnswer(this.answer().answerId, valueToSubmit).subscribe({
      next: () => {
        this.message.success('Vote registered');
        this.updateRequired.emit();
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to vote.'),
    });
  }

  // ✅ CORRECTED AND SIMPLIFIED
  markAsSolution(): void {
    this.answerService.markAsSolution(this.answer().answerId).subscribe({
      next: () => {
        this.message.success('Answer marked as solution!');
        // Just like handleVote(), we only emit to the parent.
        this.updateRequired.emit();
      },
      error: (err) => this.message.error(err.error?.message || 'Action failed.'),
    });
  }

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
    };
    this.commentService.createComment(this.answer().answerId.toString(), request, this.commentFilesToUpload()).subscribe({
      next: () => {
        this.commentForm.reset();
        this.commentFileList.set([]);
        this.commentFilesToUpload.set([]);
        this.isCommentFormVisible.set(false);
        this.message.success('Comment posted');
        this.updateRequired.emit();
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to post comment.'),
    });
  }

  // --- Delete Logic ---
  deleteAnswer(): void {
    this.modal.confirm({
      nzTitle: 'Delete this answer?',
      nzContent: 'This action cannot be undone.',
      nzOkText: 'Delete',
      nzOkDanger: true,
      nzOnOk: () =>
        this.answerService.deleteAnswer(this.answer().answerId).subscribe({
          next: () => {
            this.message.success('Answer deleted');
            this.updateRequired.emit();
          },
          error: (err) => this.message.error(err.error?.message || 'Failed to delete answer.'),
        }),
    });
  }
  
  // --- Update Logic ---
  handleSave(event: PostEditSaveEvent): void {
    const request = {
      body: event.body,
      attachmentIdsToDelete: event.attachmentIdsToDelete,
    };
    this.answerService.updateAnswer(this.answer().answerId, request, event.newFiles).subscribe({
      next: () => {
        this.message.success('Answer updated');
        this.isEditing.set(false);
        this.updateRequired.emit(); // Refresh data from parent
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to update answer.'),
    });
  }
}

