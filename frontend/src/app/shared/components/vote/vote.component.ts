import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
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
  score = input.required<number>();
  userVote = input.required<number>(); // -1, 0, or 1
  isVertical = input<boolean>(true); // Default to vertical layout

  // --- OUTPUTS ---
  vote = output<1 | -1>();

  // --- LOGIC ---
  upvote(): void {
    // If user has already upvoted, clicking again retracts the vote (sends an upvote to be undone)
    // Otherwise, it's a new upvote.
    this.vote.emit(1);
  }

  downvote(): void {
    // If user has already downvoted, clicking again retracts the vote
    // Otherwise, it's a new downvote.
    this.vote.emit(-1);
  }
}