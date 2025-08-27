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
import { NzCommentModule } from 'ng-zorro-antd/comment';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzIconModule } from 'ng-zorro-antd/icon'; // <-- Import NzIconModule

@Component({
  selector: 'app-comment-item',
  standalone: true,
  imports: [
    CommentItemComponent, ReactiveFormsModule, RouterLink,
    TimeAgoPipe, VoteComponent, NzAvatarModule,
    NzButtonModule, NzFormModule, NzInputModule, NzIconModule
  ],
  templateUrl: './comment-item.component.html',
  styleUrl: './comment-item.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CommentItemComponent {
  // --- Injections ---
  private readonly fb = inject(FormBuilder);
  private readonly voteService = inject(VoteService);
  private readonly commentService = inject(CommentService);
  private readonly message = inject(NzMessageService);
  public readonly authService = inject(AuthService); // Make public for template access

  // --- Inputs & Outputs ---
  comment = input.required<Comment>();
  answerId = input.required<number>();
  updateRequired = output<void>();

  // --- State ---
  isReplyFormVisible = signal(false);
  isCollapsed = signal(false);
  replyForm = this.fb.group({
    body: ['', [Validators.required, Validators.minLength(2)]],
  });

  // --- Logic ---
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
    if (this.replyForm.invalid) return;
    const request: CommentCreateRequest = {
      body: this.replyForm.value.body!,
      parentCommentId: this.comment().commentId,
    };
    this.commentService.createComment(this.answerId(), request, []).subscribe({
      next: () => {
        this.replyForm.reset();
        this.isReplyFormVisible.set(false);
        this.message.success('Reply posted');
        this.updateRequired.emit();
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to post reply.'),
    });
  }
}