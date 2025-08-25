import { ChangeDetectionStrategy, Component, EventEmitter, input, output } from '@angular/core';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';

@Component({
  selector: 'app-vote',
  standalone: true,
  imports: [NzButtonModule, NzIconModule],
  templateUrl: './vote.component.html',
  styleUrl: './vote.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VoteComponent {
  // --- INPUTS ---
  // The parent component will provide these values.
  score = input.required<number>(); // The total score of the post
  userVote = input<number>(0);     // The current user's vote (-1, 0, or 1)

  // --- OUTPUT ---
  // This emits an event when the user clicks a vote button.
  vote = output<number>();

  onVote(newVote: number): void {
    // If the user clicks the same button again, it's a "cancel" vote (0).
    // Otherwise, it's the new vote value.
    const voteValue = this.userVote() === newVote ? 0 : newVote;
    this.vote.emit(voteValue);
  }
}