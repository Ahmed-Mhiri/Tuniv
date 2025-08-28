import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
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
    CommentItemComponent, // For recursive nesting of comments
    ReactiveFormsModule, 
    RouterLink, 
    TimeAgoPipe,
    VoteComponent, 
    NzAvatarModule, 
    NzButtonModule,
    NzFormModule, 
    NzInputModule, 
    NzIconModule, 
    NzUploadModule,
    NzToolTipModule, 
    NzSpaceModule
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

  comment = input.required<Comment>();
  answerId = input.required<number>();
  updateRequired = output<void>();

  isReplyFormVisible = signal(false);
  isCollapsed = signal(false);
  replyForm = this.fb.group({
    body: ['', [Validators.required, Validators.minLength(2)]],
  });
  
  readonly replyFileList = signal<NzUploadFile[]>([]);
  readonly replyFilesToUpload = signal<File[]>([]);

  preventAutoUpload = () => false;

  toggleCollapse(): void { 
    this.isCollapsed.set(!this.isCollapsed()); 
  }

  handleVote(voteValue: number): void {
    const valueToSubmit = voteValue >= 0 ? 1 : -1;
    // Assuming voteOnComment service expects a number for commentId
    this.voteService.voteOnComment(this.comment().commentId, valueToSubmit).subscribe({
      next: () => {
        this.message.success('Vote registered');
        this.updateRequired.emit();
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to vote.'),
    });
  }

  handleReplyFileChange({ file, fileList }: NzUploadChangeParam): void {
    console.log('--- 1. File Change Event Triggered ---');
    console.log('File that changed:', file);
    console.log('Full list of files from uploader:', fileList);

    // This is a critical step for manual uploads.
    // We ensure the file status is 'done' so it's treated as a stable item in the list.
    const processedList = fileList.map(f => ({ ...f, status: 'done' as const }));
    console.log('2. List after setting status to "done":', processedList);
    
    this.replyFileList.set(processedList);
    console.log('3. UI File List signal has been updated. The preview should now appear.');
    
    const filesToSubmit = processedList
      .map(f => f.originFileObj as File)
      .filter(f => !!f);

    this.replyFilesToUpload.set(filesToSubmit);
    console.log('4. Raw files for submission have been updated:', filesToSubmit);
    console.log('--- File Handling Complete ---');
  }
  
  
  removeReplyFile(fileToRemove: NzUploadFile): void {
    console.log('--- Remove File Clicked ---', fileToRemove.name);
    const newFileList = this.replyFileList().filter(f => f.uid !== fileToRemove.uid);
    this.handleReplyFileChange({ file: fileToRemove, fileList: newFileList });
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

    // Convert answerId from number to string for the service call
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