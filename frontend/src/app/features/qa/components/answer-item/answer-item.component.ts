import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { Answer, CommentCreateRequest } from '../../../../shared/models/qa.model';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';
import { VoteComponent } from '../../../../shared/components/vote/vote.component';
import { CommentItemComponent } from '../comment-item/comment-item.component';
import { AnswerService } from '../../services/answer.service';
import { VoteService } from '../../services/vote.service';

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
import { CommentService } from '../../services/comment.service';

@Component({
  selector: 'app-answer-item',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, TimeAgoPipe, VoteComponent,
    CommentItemComponent, NzAvatarModule, NzButtonModule,
    NzFormModule, NzInputModule, NzIconModule, NzUploadModule,
    NzToolTipModule, NzSpaceModule
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

  answer = input.required<Answer>();
  isQuestionAuthor = input<boolean>(false);
  updateRequired = output<void>();

  isCommentFormVisible = signal(false);
  commentForm = this.fb.group({
    body: ['', [Validators.required, Validators.minLength(2)]],
  });
  
  readonly commentFileList = signal<NzUploadFile[]>([]);
  readonly commentFilesToUpload = signal<File[]>([]);

  preventAutoUpload = () => false;

  handleVote(voteValue: number): void {
    const valueToSubmit = voteValue >= 0 ? 1 : -1;
    this.voteService.voteOnAnswer(this.answer().answerId, valueToSubmit).subscribe({
      next: () => {
        this.message.success('Vote registered');
        this.updateRequired.emit();
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to vote.'),
    });
  }

  markAsSolution(): void {
    this.answerService.markAsSolution(this.answer().answerId).subscribe({
      next: () => {
        this.message.success('Answer marked as solution!');
        this.updateRequired.emit();
      },
      error: (err) => this.message.error(err.error?.message || 'Action failed.'),
    });
  }

  handleCommentFileChange({ fileList }: NzUploadChangeParam): void {
    const validatedList = fileList.filter(f => {
      const isLt5M = (f.size ?? 0) / 1024 / 1024 < 5;
      if (!isLt5M) {
        this.message.error(`File '${f.name}' is too large (max 5MB).`);
        return false;
      }
      return true;
    });
    this.commentFileList.set(validatedList);
    this.commentFilesToUpload.set(
      validatedList.map(f => f.originFileObj as File).filter(f => !!f)
    );
  }

  removeCommentFile(fileToRemove: NzUploadFile): void {
    const newFileList = this.commentFileList().filter(f => f.uid !== fileToRemove.uid);
    this.handleCommentFileChange({ file: fileToRemove, fileList: newFileList });
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

    // FIX 3: Convert number to string for the comment service
    this.commentService.createComment(
      this.answer().answerId.toString(), 
      request, 
      this.commentFilesToUpload()
    ).subscribe({
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
}


  