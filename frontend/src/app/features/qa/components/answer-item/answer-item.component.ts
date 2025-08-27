import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { Answer, CommentCreateRequest } from '../../../../shared/models/qa.model';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';
import { VoteComponent } from '../../../../shared/components/vote/vote.component';
import { CommentItemComponent } from '../comment-item/comment-item.component';
import { AnswerService } from '../../services/answer.service';
import { CommentService } from '../../services/comment.service';
import { VoteService } from '../../services/vote.service';

// NG-ZORRO Modules
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMessageService } from 'ng-zorro-antd/message';

@Component({
  selector: 'app-answer-item',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, TimeAgoPipe, VoteComponent,
    CommentItemComponent, NzAvatarModule, NzButtonModule,
    NzFormModule, NzInputModule, NzIconModule,
  ],
  templateUrl: './answer-item.component.html',
  styleUrl: './answer-item.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnswerItemComponent {
  // --- Injections ---
  private readonly fb = inject(FormBuilder);
  private readonly voteService = inject(VoteService);
  private readonly commentService = inject(CommentService);
  private readonly answerService = inject(AnswerService);
  private readonly message = inject(NzMessageService);
  public readonly authService = inject(AuthService);

  // --- Inputs & Outputs ---
  answer = input.required<Answer>();
  isQuestionAuthor = input<boolean>(false);
  updateRequired = output<void>();

  // --- State ---
  isCommentFormVisible = signal(false);
  commentForm = this.fb.group({
    body: ['', [Validators.required, Validators.minLength(2)]],
  });

  // --- THIS IS THE FIX ---
  // Ensure this method accepts a generic 'number' and coerces it.
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

  submitComment(): void {
    if (this.commentForm.invalid) return;
    const request: CommentCreateRequest = {
      body: this.commentForm.value.body!,
      parentCommentId: null,
    };
    this.commentService.createComment(this.answer().answerId, request, []).subscribe({
      next: () => {
        this.commentForm.reset();
        this.isCommentFormVisible.set(false);
        this.message.success('Comment posted');
        this.updateRequired.emit();
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to post comment.'),
    });
  }
}