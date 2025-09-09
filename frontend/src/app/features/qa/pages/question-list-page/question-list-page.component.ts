import { ChangeDetectionStrategy, Component, OnInit, TemplateRef, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { switchMap } from 'rxjs';

import { Question, QuestionSummaryDto } from '../../../../shared/models/qa.model';
import { QuestionService } from '../../services/question.service';
import { AuthService } from '../../../../core/services/auth.service';

// NG-ZORRO Modules
import { NzPageHeaderModule } from 'ng-zorro-antd/page-header';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzModalModule, NzModalRef, NzModalService } from 'ng-zorro-antd/modal'; // <-- Import NzModalRef

// Components
import { QuestionListItemComponent } from '../../components/question-list-item/question-list-item';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';


@Component({
  selector: 'app-question-list-page',
  standalone: true,
  imports: [
    CommonModule, RouterLink, QuestionListItemComponent,
    NzPageHeaderModule, NzButtonModule, NzIconModule,
    NzEmptyModule, SpinnerComponent, NzGridModule, NzModalModule
  ],
  templateUrl: './question-list-page.component.html',
  styleUrl: './question-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionListPageComponent implements OnInit {
  // --- Dependencies ---
  private readonly route = inject(ActivatedRoute);
  private readonly qaService = inject(QuestionService);
  private readonly authService = inject(AuthService);
  public readonly router = inject(Router);
  private readonly modal = inject(NzModalService);

  // --- State ---
readonly questions = signal<QuestionSummaryDto[]>([]);
  readonly moduleName = signal<string>('Module');
  readonly moduleId = signal<number | null>(null);
  readonly isLoading = signal(true);

  // --- ADDED ---
  // Property to hold a reference to the currently open modal
  private activeModal: NzModalRef | null = null;

  ngOnInit(): void {
  this.route.paramMap.pipe(
    switchMap(params => {
      const id = Number(params.get('moduleId'));
      this.moduleId.set(id);
      this.isLoading.set(true);
      // This service call now correctly returns the new DTO type
      return this.qaService.getQuestionsByModule(id);
    })
  ).subscribe(questionPage => {
    // The type from the page content now matches the signal's type
    this.questions.set(questionPage.content);
    this.isLoading.set(false);
  });
}

  /**
   * --- MODIFIED ---
   * Stores the modal reference so it can be closed by other component methods.
   */
  handleAskQuestionClick(modalFooter: TemplateRef<{}>): void {
    if (this.authService.isUserLoggedIn()) {
      this.router.navigate(['/qa/ask'], { queryParams: { module: this.moduleId() } });
    } else {
      // Store the reference when creating the modal
      this.activeModal = this.modal.create({
        nzTitle: 'Login Required',
        nzContent: 'You must be logged in to ask a question. Please log in or create a free account.',
        nzFooter: modalFooter,
        nzMaskClosable: true,
        nzClosable: true
      });

      // Clean up the reference when the modal is closed (via 'X', mask click, etc.)
      this.activeModal.afterClose.subscribe(() => {
        this.activeModal = null;
      });
    }
  }

  // --- ADDED ---
  // New methods to be called from the modal footer buttons

  handleCancel(): void {
    this.activeModal?.destroy();
  }

  handleRegister(): void {
    this.activeModal?.destroy();
    this.router.navigate(['/register']);
  }

  handleLogin(): void {
    this.activeModal?.destroy();
    this.router.navigate(['/login']);
  }
}