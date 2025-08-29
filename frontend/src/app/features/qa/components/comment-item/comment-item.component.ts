import { ChangeDetectionStrategy, ChangeDetectorRef, Component, inject, input, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, of } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { Comment, CommentCreateRequest } from '../../../../shared/models/qa.model';
import { CommentService } from '../../services/comment.service';
import { VoteService } from '../../services/vote.service';
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

@Component({
  selector: 'app-comment-item',
  standalone: true,
  imports: [
    CommentItemComponent, ReactiveFormsModule, RouterLink, TimeAgoPipe, VoteComponent,
    NzAvatarModule, NzButtonModule, NzFormModule, NzInputModule, NzIconModule,
    NzUploadModule, NzToolTipModule, NzSpaceModule
  ],
  templateUrl: './comment-item.component.html',
  styleUrl: './comment-item.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommentItemComponent {
  private readonly fb = inject(FormBuilder);
  private readonly voteService = inject(VoteService);
  private readonly commentService = inject(CommentService);
  private readonly message = inject(NzMessageService);
  public readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  comment = input.required<Comment>();
  answerId = input.required<number>();
  updateRequired = output<void>();

  isReplyFormVisible = signal(false);
  isCollapsed = signal(false);
   readonly replyFileList = signal<NzUploadFile[]>([]);
  readonly replyFilesToUpload = signal<File[]>([]);

  private atLeastOneFieldValidator = (control: AbstractControl): ValidationErrors | null => {
    const body = control.get('body')?.value;
    const hasFiles = this.replyFileList().length > 0;
    // Return an error if the body is empty AND there are no files.
    return !body?.trim() && !hasFiles ? { atLeastOneRequired: true } : null;
  };
  
  replyForm = this.fb.group({
    body: [''], // Validators removed from here...
  }, {
    validators: this.atLeastOneFieldValidator // ...and added to the group.
  });

 

  // =========================================================================
  // ✅ THE FINAL FIX
  // =========================================================================
  beforeUpload = (file: NzUploadFile): Observable<boolean> => {
    // We use the 'file' object itself, as it is the File instance we need.
    // The `as unknown as File` cast is needed to satisfy TypeScript.
    this.replyFilesToUpload.update(list => [...list, file as unknown as File]);
    
    // We also add it to the list that controls the UI preview.
    this.replyFileList.update(list => [...list, file]);

    this.cdr.detectChanges();
    return of(false); // Prevent automatic upload
  };

  handleReplyFileChange(info: NzUploadChangeParam): void {
    // This is now only needed to handle removal from the UI.
    if (info.type === 'removed') {
      this.removeReplyFile(info.file);
    }
  }

  removeReplyFile(fileToRemove: NzUploadFile): void {
    // We remove the file from both lists using its unique ID (uid).
    this.replyFileList.update(list => list.filter(f => f.uid !== fileToRemove.uid));
    this.replyFilesToUpload.update(list => (list as unknown as NzUploadFile[]).filter(f => f.uid !== fileToRemove.uid) as unknown as File[]);
    this.cdr.detectChanges();
  }
  // =========================================================================

  toggleCollapse(): void {
    this.isCollapsed.set(!this.isCollapsed());
  }

  handleVote(voteValue: number): void {
    const valueToSubmit = voteValue >= 0 ? 1 : -1;
    this.voteService.voteOnComment(this.comment().commentId, valueToSubmit).subscribe({
      next: () => {
        this.message.success('Vote registered');
        this.updateRequired.emit();
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to vote.'),
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
    };
    this.commentService.createComment(this.answerId().toString(), request, this.replyFilesToUpload()).subscribe({
      next: () => {
        this.replyForm.reset();
        this.replyFileList.set([]);
        this.replyFilesToUpload.set([]);
        this.isReplyFormVisible.set(false);
        this.message.success('Reply posted');
        this.updateRequired.emit();
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to post reply.'),
    });
  }
}