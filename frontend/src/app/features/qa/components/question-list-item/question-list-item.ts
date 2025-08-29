import { Component, computed, input } from '@angular/core';
import { Question } from '../../../../shared/models/qa.model';
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
  question = input.required<Question>();

  // A computed signal to determine the visual status of the answer count
  answerStatus = computed(() => {
    const q = this.question();
    if (q.answers.some(a => a.isSolution)) {
      return 'has-solution'; // Has an accepted answer
    }
    if (q.answers.length > 0) {
      return 'has-answers'; // Has answers, but none are the accepted solution
    }
    return 'no-answers'; // No answers yet
  });
}
