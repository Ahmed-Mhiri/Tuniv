import { ChangeDetectionStrategy, Component, output, inject, signal } from '@angular/core';
// IMPORTANT: AbstractControl and ValidationErrors are needed for the custom validator
import { FormBuilder, ReactiveFormsModule, Validators, FormGroup, AbstractControl, ValidationErrors } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { NzUploadFile, NzUploadChangeParam, NzUploadModule } from 'ng-zorro-antd/upload';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { NzInputModule } from 'ng-zorro-antd/input';

export interface AnswerSubmitEvent {
  body: string;
  files: File[];
}

@Component({
  selector: 'app-answer-form',
  standalone: true,
  imports: [
    ReactiveFormsModule, NzButtonModule, NzFormModule,
    NzTypographyModule, NzUploadModule, NzIconModule,
    NzToolTipModule, NzSpaceModule, NzInputModule
  ],
  templateUrl: './answer-form.html',
  styleUrl: './answer-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnswerFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly message = inject(NzMessageService);
  readonly answerSubmit = output<AnswerSubmitEvent>();
  
  readonly fileList = signal<NzUploadFile[]>([]);
  readonly filesToUpload = signal<File[]>([]);
  readonly isSubmitting = signal(false);

  // ===============================================================
  // START OF FIX
  // ===============================================================

  // 1. Define the custom validator as a class property (using an arrow function).
  // This validator checks if either the body has text or if files have been uploaded.
  private atLeastOneFieldValidator = (control: AbstractControl): ValidationErrors | null => {
    const body = control.get('body')?.value;
    const hasFiles = this.fileList().length > 0;

    // If the body is empty AND there are no files, return an error.
    if (!body?.trim() && !hasFiles) {
      return { atLeastOneRequired: true };
    }
    
    // Otherwise, the form is valid.
    return null; 
  };

  // 2. Update the form definition.
  readonly answerForm = this.fb.group({
    // Remove the old validators from the 'body' control.
    body: [''],
  }, {
    // Add our new custom validator to the entire form group.
    validators: this.atLeastOneFieldValidator
  });

  // ===============================================================
  // END OF FIX
  // ===============================================================

  handleFileChange({ fileList }: NzUploadChangeParam): void {
    const validatedList = fileList.filter(f => {
      const isLt5M = (f.size ?? 0) / 1024 / 1024 < 5;
      if (!isLt5M) {
        this.message.error(`File '${f.name}' is too large. Must be smaller than 5MB!`);
        return false;
      }
      return true;
    });

    this.fileList.set(validatedList);
    this.filesToUpload.set(
      validatedList.map(f => f.originFileObj as File).filter(f => !!f)
    );

    // 3. We need to re-validate the form whenever the file list changes.
    this.answerForm.updateValueAndValidity();
  }

  removeFile(fileToRemove: NzUploadFile): void {
    const newFileList = this.fileList().filter(f => f.uid !== fileToRemove.uid);
    // handleFileChange already re-validates, so we are covered here.
    this.handleFileChange({ file: fileToRemove, fileList: newFileList });
  }

  submit(): void {
    if (this.answerForm.invalid) {
      this.answerForm.markAllAsTouched();
      return;
    }
    
    this.answerSubmit.emit({
      body: this.answerForm.value.body!,
      files: this.filesToUpload(),
    });
    
    this.answerForm.reset();
    this.fileList.set([]);
    this.filesToUpload.set([]);
  }
}