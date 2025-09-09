import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { map, Observable, of } from 'rxjs';

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
import { Module, ModuleDetail, University } from '../../../../shared/models/university.model';
import { QuestionCreateRequest } from '../../../../shared/models/qa.model';
import { ModuleService } from '../../../university/services/module.service';
import { UniversityService } from '../../../university/services/university.service';
import { NzDividerComponent } from 'ng-zorro-antd/divider';
import { NzAlertComponent } from 'ng-zorro-antd/alert';

@Component({
  selector: 'app-ask-question-page',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink, SpinnerComponent,
    NzButtonModule, NzFormModule, NzInputModule, NzTypographyModule,
    NzUploadModule, NzIconModule, NzSelectModule,NzDividerComponent,NzAlertComponent
  ],
  templateUrl: './ask-question-page.component.html',
  styleUrl: './ask-question-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AskQuestionPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly questionService = inject(QuestionService);
  private readonly moduleService = inject(ModuleService);
  private readonly universityService = inject(UniversityService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly message = inject(NzMessageService);
  private readonly cdr = inject(ChangeDetectorRef);

  // --- State Signals ---
  readonly universities = signal<University[]>([]);
  readonly modules = signal<Module[]>([]);
  readonly isLoading = signal(true);
  readonly isLoadingModules = signal(false);
  readonly fileList = signal<NzUploadFile[]>([]);
  readonly filesToUpload = signal<File[]>([]);
  readonly isSubmitting = signal(false);
  
  // --- Contextual Posting State ---
  readonly isContextualPost = signal(false);
  readonly contextInfo = signal<{ uniName: string, modName: string } | null>(null);
  readonly userMustJoin = signal(false);
  readonly isJoining = signal(false);
  readonly universityToJoin = signal<{ id: number; name: string } | null>(null);

  // =========================================================================
  // ✅ FIX 1: Add a signal to "remember" the module ID from the URL
  // =========================================================================
  private readonly contextualModuleId = signal<number | null>(null);

  readonly questionForm = this.fb.group({
    universityId: [null as number | null, [Validators.required]],
    moduleId: [null as number | null, [Validators.required]],
    title: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(150)]],
    body: ['', [Validators.required, Validators.minLength(20)]],
  });

  ngOnInit(): void {
    const moduleIdParam = this.route.snapshot.queryParamMap.get('module');
    if (moduleIdParam) {
      const moduleId = Number(moduleIdParam);
      // ✅ FIX 2: Store the module ID as soon as the page loads
      this.contextualModuleId.set(moduleId);
      this.handleContextualPost(moduleId);
    } else {
      this.handleGlobalPost();
    }
  }

  private handleContextualPost(moduleId: number): void {
    this.isContextualPost.set(true);
    this.isLoading.set(true);
    this.userMustJoin.set(false); // Reset join state on each check

    this.moduleService.getModuleById(moduleId).subscribe({
      next: (moduleDetails: ModuleDetail) => {
        if (moduleDetails.university.isMember) {
          this.questionForm.get('universityId')?.disable();
          this.questionForm.get('moduleId')?.disable();
          this.questionForm.patchValue({
            universityId: moduleDetails.university.universityId,
            moduleId: moduleDetails.moduleId
          });
          this.contextInfo.set({
            uniName: moduleDetails.university.name,
            modName: moduleDetails.name,
          });
        } else {
          this.userMustJoin.set(true);
          this.universityToJoin.set({
            id: moduleDetails.university.universityId,
            name: moduleDetails.university.name
          });
        }
        this.isLoading.set(false);
      },
      error: () => this.message.error('Could not load module information.')
    });
  }
  
  joinUniversity(): void {
    const uni = this.universityToJoin();
    // ✅ FIX 3: Get the remembered module ID from our signal
    const modId = this.contextualModuleId();
    if (!uni || !modId) return;

    this.isJoining.set(true);
    this.universityService.joinUniversity(uni.id).subscribe({
      next: () => {
        this.message.success(`Successfully joined ${uni.name}!`);
        // Re-run the logic using the correct, remembered module ID
        this.handleContextualPost(modId);
      },
      error: (err) => this.message.error(err.error?.message || 'Failed to join university.'),
      complete: () => this.isJoining.set(false),
    });
  }
  
  // ... handleGlobalPost, file handling, and submitQuestion methods are unchanged ...

  private handleGlobalPost(): void {
    this.isContextualPost.set(false);
    this.isLoading.set(true); // Start loading

    // ✅ REPLACED THE OLD, INEFFICIENT CODE
    this.universityService.getJoinedUniversities().subscribe({
      next: (joinedUniversities) => {
        this.universities.set(joinedUniversities);
        this.isLoading.set(false);
      },
      error: () => {
        this.message.error('Failed to load your universities.');
        this.isLoading.set(false);
      },
    });

    // This valueChanges subscription can remain exactly as it is
    this.questionForm.get('universityId')?.valueChanges.subscribe(universityId => {
  this.questionForm.get('moduleId')?.reset();
  this.modules.set([]);
  if (universityId) {
    this.isLoadingModules.set(true);
    // ✅ Call the new, correct method
    this.moduleService.getModulesForDropdown(universityId).subscribe({
      next: (data) => this.modules.set(data), // 'data' is now the Module[] array you expect
      error: () => this.message.error('Failed to load modules for this university.'),
      complete: () => this.isLoadingModules.set(false),
    });
  }
});
  }


  beforeUpload = (file: NzUploadFile): Observable<boolean> => {
    this.filesToUpload.update(list => [...list, file as unknown as File]);
    this.fileList.update(list => [...list, file]);
    this.cdr.detectChanges();
    return of(false);
  };

  handleFileChange(info: NzUploadChangeParam): void {
    if (info.type === 'removed') {
      this.removeFile(info.file);
    }
  }

  removeFile(fileToRemove: NzUploadFile): void {
    this.fileList.update(list => list.filter(f => f.uid !== fileToRemove.uid));
    this.filesToUpload.update(list => (list as unknown as NzUploadFile[]).filter(f => f.uid !== fileToRemove.uid) as unknown as File[]);
    this.cdr.detectChanges();
  }
  
  submitQuestion(): void {
    if (this.questionForm.invalid) {
      Object.values(this.questionForm.controls).forEach(control => {
        control.markAsDirty();
        control.updateValueAndValidity({ onlySelf: true });
      });
      return;
    }

    this.isSubmitting.set(true);
    const formValue = this.questionForm.getRawValue();

    // ✅ FIX 1: Add the missing 'moduleId' to the request object.
    const questionData: QuestionCreateRequest = {
      title: formValue.title!,
      body: formValue.body!,
      moduleId: formValue.moduleId!
    };

    // ✅ FIX 2: Call the service with the new 2-argument signature.
    this.questionService.createQuestion(
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