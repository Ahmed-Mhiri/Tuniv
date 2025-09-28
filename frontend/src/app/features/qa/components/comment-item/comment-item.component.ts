import { ChangeDetectionStrategy, ChangeDetectorRef, Component, computed, inject, input, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, of } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { Comment, CommentCreateRequest, Question } from '../../../../shared/models/qa.model';
import { CommentService } from '../../services/comment.service';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';
import { VoteComponent } from '../../../../shared/components/vote/vote.component';

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
import { SafeHtmlPipe } from '../../../../shared/pipes/safe-html.pipe';

@Component({
  selector: 'app-comment-item',
  standalone: true,
  imports: [
    CommentItemComponent, ReactiveFormsModule, RouterLink, TimeAgoPipe, VoteComponent,
    NzAvatarModule, NzButtonModule, NzFormModule, NzInputModule, NzIconModule,
    NzUploadModule, NzToolTipModule, NzSpaceModule,PostEditFormComponent,SafeHtmlPipe
  ],
  templateUrl: './comment-item.component.html',
  styleUrl: './comment-item.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommentItemComponent {
  // --- Injections ---
  private readonly fb = inject(FormBuilder);
  private readonly commentService = inject(CommentService);
  private readonly message = inject(NzMessageService);
  public readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly modal = inject(NzModalService);

  // ✅ REMOVED VoteService
  
  // --- Inputs & Outputs ---
  comment = input.required<Comment>();
  answerId = input.required<number>();
  
  updated = output<Question>();
  deleted = output<void>();

  // --- State ---
  isReplyFormVisible = signal(false);
  isCollapsed = signal(false);
  isEditing = signal(false);
  readonly replyFileList = signal<NzUploadFile[]>([]);
  readonly replyFilesToUpload = signal<File[]>([]);

  // --- Computed State ---
  readonly isCurrentUserAuthor = computed(() =>
    this.authService.currentUser()?.userId === this.comment().author.userId
  );

  // --- Forms & Validation ---
  private atLeastOneFieldValidator = (control: AbstractControl): ValidationErrors | null => {
    const body = control.get('body')?.value;
    const hasFiles = this.replyFileList().length > 0;
    return !body?.trim() && !hasFiles ? { atLeastOneRequired: true } : null;
  };
  replyForm = this.fb.group({ body: [''] }, { validators: this.atLeastOneFieldValidator });
  
  // --- Event Handlers ---

  toggleCollapse(): void {
    this.isCollapsed.set(!this.isCollapsed());
  }

  handleVote(voteValue: 1 | -1): void {
    // ✅ UPDATED: Call CommentService's voteOnComment method with the required answerId
    this.commentService.voteOnComment(this.answerId(), this.comment().commentId, voteValue).subscribe({
      next: (updatedQuestion) => this.updated.emit(updatedQuestion),
      error: (err) => this.message.error(err.error?.message || 'Failed to vote.'),
    });
  }

  deleteComment(): void {
    this.modal.confirm({
      nzTitle: 'Delete this comment?',
      nzContent: 'This action cannot be undone.',
      nzOkText: 'Delete',
      nzOkDanger: true,
      nzOnOk: () =>
        // ✅ UPDATED: Call CommentService with the required answerId
        this.commentService.deleteComment(this.answerId(), this.comment().commentId).subscribe({
          next: () => this.deleted.emit(),
          error: (err) => this.message.error(err.error?.message || 'Failed to delete comment.'),
        }),
    });
  }

  handleSave(event: PostEditSaveEvent): void {
    const request = {
      body: event.body,
      attachmentIdsToDelete: event.attachmentIdsToDelete,
    };
    // ✅ UPDATED: Call CommentService with the required answerId
    this.commentService.updateComment(this.answerId(), this.comment().commentId, request, event.newFiles).subscribe({
      next: (updatedQuestion) => {
        this.isEditing.set(false);
        this.updated.emit(updatedQuestion);
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to update comment.'),
    });
  }

  submitReply(): void {
    if (this.replyForm.invalid) {
      Object.values(this.replyForm.controls).forEach(control => {
        control.markAsDirty();
        control.updateValueAndValidity({ onlySelf: true });
      });
      return;
    }
    const request: CommentCreateRequest = {
      body: this.replyForm.value.body!,
      parentCommentId: this.comment().commentId,
      answerId: this.answerId(),
    };
    // ✅ UPDATED: Call CommentService with the required answerId
    this.commentService.createComment(this.answerId(), request, this.replyFilesToUpload()).subscribe({
      next: (updatedQuestion) => {
        this.replyForm.reset();
        this.replyFileList.set([]);
        this.replyFilesToUpload.set([]);
        this.isReplyFormVisible.set(false);
        this.updated.emit(updatedQuestion);
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to post reply.'),
    });
  }

  // --- File Handling for Replies (Unchanged) ---
  beforeUpload = (file: NzUploadFile): Observable<boolean> => {
    this.replyFilesToUpload.update(list => [...list, file as unknown as File]);
    this.replyFileList.update(list => [...list, file]);
    this.replyForm.updateValueAndValidity();
    this.cdr.detectChanges();
    return of(false);
  };

  handleReplyFileChange(info: NzUploadChangeParam): void {
    if (info.type === 'removed') {
      this.removeReplyFile(info.file);
    }
  }

  removeReplyFile(fileToRemove: NzUploadFile): void {
    this.replyFileList.update(list => list.filter(f => f.uid !== fileToRemove.uid));
    this.replyFilesToUpload.update(list => (list as unknown as NzUploadFile[]).filter(f => f.uid !== fileToRemove.uid) as unknown as File[]);
    this.replyForm.updateValueAndValidity();
    this.cdr.detectChanges();
  }
}