import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { switchMap } from 'rxjs';

import { Question } from '../../../../shared/models/qa.model';

// NG-ZORRO Modules
import { NzPageHeaderModule } from 'ng-zorro-antd/page-header';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { QuestionListItemComponent } from '../../components/question-list-item/question-list-item';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-question-list-page',
  standalone: true,
  imports: [
    CommonModule, RouterLink, QuestionListItemComponent,
    NzPageHeaderModule, NzButtonModule, NzIconModule,
    NzEmptyModule, NzSpinModule, NzGridModule
  ],
  templateUrl: './question-list-page.component.html',
  styleUrl: './question-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionListPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly qaService = inject(QuestionService);

  readonly questions = signal<Question[]>([]);
  readonly moduleName = signal<string>('Module'); // Default name
  readonly moduleId = signal<number | null>(null);
  readonly isLoading = signal(true);

  ngOnInit(): void {
    this.route.paramMap.pipe(
      switchMap(params => {
        const id = Number(params.get('moduleId'));
        this.moduleId.set(id);
        this.isLoading.set(true);
        // In a real app, you might fetch module details here too
        // to get the name, e.g., from a ModuleService.
        return this.qaService.getQuestionsByModule(id);
      })
    ).subscribe(questionPage => {
      this.questions.set(questionPage.content); // Assuming the service returns a Page object
      this.isLoading.set(false);
    });
  }
}