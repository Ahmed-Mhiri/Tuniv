// answer-form.ts

import { ChangeDetectionStrategy, Component, output, inject, signal, input } from '@angular/core'; // 👈 1. Import 'input'
import { FormBuilder, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { NzUploadFile, NzUploadModule } from 'ng-zorro-antd/upload';
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
export class AnswerForm {
  private readonly fb = inject(FormBuilder);
  private readonly message = inject(NzMessageService);
  readonly answerSubmit = output<AnswerSubmitEvent>();
  
  readonly fileList = signal<NzUploadFile[]>([]);
  readonly filesToUpload = signal<File[]>([]);
  
  // 👇 2. Change from 'signal(false)' to 'input<boolean>(false)'
  isSubmitting = input<boolean>(false);

  private atLeastOneFieldValidator = (control: AbstractControl): ValidationErrors | null => {
    const body = control.get('body')?.value;
    const hasFiles = this.fileList().length > 0;
    if (!body?.trim() && !hasFiles) {
      return { atLeastOneRequired: true };
    }
    return null; 
  };

  readonly answerForm = this.fb.group({
    body: [''],
  }, {
    validators: this.atLeastOneFieldValidator
  });

  beforeUpload = (file: NzUploadFile): boolean => {
    const isLt5M = (file.size ?? 0) / 1024 / 1024 < 5;
    if (!isLt5M) {
      this.message.error(`File '${file.name}' is too large. Must be smaller than 5MB!`);
      return false;
    }

    this.fileList.update(list => [...list, file]);
    this.filesToUpload.update(list => [...list, file as unknown as File]);
    
    this.answerForm.updateValueAndValidity();
    return false;
  };

  removeFile(fileToRemove: NzUploadFile): void {
    const newFileList = this.fileList().filter(f => f.uid !== fileToRemove.uid);
    this.fileList.set(newFileList);

    const newFilesToUpload = newFileList
      .map(f => f.originFileObj as File)
      .filter(f => !!f);
    this.filesToUpload.set(newFilesToUpload);

    this.answerForm.updateValueAndValidity();
  }

  submit(): void {
    if (this.answerForm.invalid) {
      this.answerForm.markAllAsTouched();
      return;
    }

    console.log('--- DEBUG: Submitting from AnswerFormComponent ---');
    console.log('Files to submit:', this.filesToUpload());
    console.log('-------------------------------------------');

    this.answerSubmit.emit({
      body: this.answerForm.value.body!,
      files: this.filesToUpload(),
    });
    
    this.answerForm.reset();
    this.fileList.set([]);
    this.filesToUpload.set([]);
  }
}