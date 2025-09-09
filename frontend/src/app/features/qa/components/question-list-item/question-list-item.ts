import { Component, computed, input } from '@angular/core';
import { Question, QuestionSummaryDto } from '../../../../shared/models/qa.model';
import { RouterModule } from '@angular/router';
import { TimeAgoPipe } from "../../../../shared/pipes/time-ago.pipe";
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-question-list-item',
  imports: [RouterModule, TimeAgoPipe,CommonModule],
  templateUrl: './question-list-item.html',
  styleUrl: './question-list-item.scss'
})
export class QuestionListItemComponent {
  // Receive the question object from the parent component
  question = input.required<QuestionSummaryDto>();


  // A computed signal to determine the visual status of the answer count
  answerStatus = computed(() => {
    const q = this.question();
    // We can no longer check for a solution here, which is a trade-off for performance.
    // The main goal is to show if there are answers or not.
    if (q.answerCount > 0) {
      return 'has-answers';
    }
    return 'no-answers';
  });
}