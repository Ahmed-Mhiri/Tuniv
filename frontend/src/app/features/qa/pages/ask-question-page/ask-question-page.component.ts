import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';

import { QuestionService } from '../../services/question.service'; // Use your existing QuestionService

// NG-ZORRO Modules
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { NzUploadChangeParam, NzUploadFile, NzUploadModule } from 'ng-zorro-antd/upload';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';
import { Module, University } from '../../../../shared/models/university.model';
import { QuestionCreateRequest } from '../../../../shared/models/qa.model';
import { ModuleService } from '../../../university/services/module.service';
import { UniversityService } from '../../../university/services/university.service';

@Component({
  selector: 'app-ask-question-page',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink, SpinnerComponent,
    NzButtonModule, NzFormModule, NzInputModule, NzTypographyModule,
    NzUploadModule, NzIconModule, NzSelectModule
  ],
  templateUrl: './ask-question-page.component.html',
  styleUrl: './ask-question-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AskQuestionPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly questionService = inject(QuestionService);
  private readonly moduleService = inject(ModuleService);
  private readonly universityService = inject(UniversityService); // For fetching universities
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute); // For reading query params
  private readonly message = inject(NzMessageService);

  readonly universities = signal<University[]>([]);
  readonly modules = signal<Module[]>([]);
  readonly isLoadingUniversities = signal(true);
  readonly isLoadingModules = signal(false);
  readonly fileList = signal<NzUploadFile[]>([]);
  readonly filesToUpload = signal<File[]>([]);
  readonly isSubmitting = signal(false);

  readonly questionForm = this.fb.group({
    universityId: [null as number | null, [Validators.required]], // New field for university
    moduleId: [null as number | null, [Validators.required]],
    title: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(150)]],
    body: ['', [Validators.required, Validators.minLength(20)]],
  });

  ngOnInit(): void {
    this.loadUniversities();
    this.setupUniversitySelectionListener();
    this.checkRouteForPreselection();
  }

  private loadUniversities(): void {
    this.universityService.getAllUniversities().subscribe({
      next: (data) => this.universities.set(data),
      error: () => this.message.error('Failed to load universities.'),
      complete: () => this.isLoadingUniversities.set(false),
    });
  }
  
  // When a university is selected, load its modules
  private setupUniversitySelectionListener(): void {
    this.questionForm.get('universityId')?.valueChanges.subscribe(universityId => {
      this.questionForm.get('moduleId')?.reset(); // Reset module when uni changes
      if (universityId) {
        this.isLoadingModules.set(true);
        this.moduleService.getModulesByUniversity(universityId).subscribe({
          next: (data) => this.modules.set(data),
          error: () => this.message.error('Failed to load modules for this university.'),
          complete: () => this.isLoadingModules.set(false),
        });
      }
    });
  }
  
  // If we came from a module page, pre-select the module
  private checkRouteForPreselection(): void {
    const moduleId = this.route.snapshot.queryParamMap.get('module');
    if (moduleId) {
      // In a full implementation, you'd fetch the module's details
      // to find its parent universityId and set both form controls.
      this.questionForm.patchValue({ moduleId: Number(moduleId) });
    }
  }

  handleFileChange({ fileList }: NzUploadChangeParam): void {
    // We use a slice to create a new array, which helps with change detection
    this.fileList.set(fileList.slice());
    this.filesToUpload.set(
      fileList.map(f => f.originFileObj as File).filter(f => !!f)
    );
  }

  submitQuestion(): void {
    if (this.questionForm.invalid) {
      // Mark all fields as touched to display validation errors
      Object.values(this.questionForm.controls).forEach(control => {
        control.markAsDirty();
        control.updateValueAndValidity({ onlySelf: true });
      });
      return;
    }

    this.isSubmitting.set(true);
    const formValue = this.questionForm.value;

    const questionData: QuestionCreateRequest = {
      title: formValue.title!,
      body: formValue.body!
    };

    this.questionService.createQuestion(
      formValue.moduleId!,
      questionData,
      this.filesToUpload()
    ).subscribe({
      next: (newQuestion) => {
        this.isSubmitting.set(false);
        this.message.success('Question posted successfully!');
        this.router.navigate(['/questions', newQuestion.questionId]);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.message.error(err.error?.message || 'Failed to post question.');
      }
    });
  }


}

