import { ChangeDetectionStrategy, Component, output, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { NzUploadFile, NzUploadModule } from 'ng-zorro-antd/upload';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { NzInputModule } from 'ng-zorro-antd/input'; // <-- Import NzInputModule

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
    NzToolTipModule, NzSpaceModule, NzInputModule // <-- Add NzInputModule
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
  readonly isSubmitting = signal(false);

  readonly answerForm = this.fb.group({
    body: ['', [Validators.required, Validators.minLength(20)]],
  });

  beforeUpload = (file: NzUploadFile): boolean => {
    const isLt5M = file.size! / 1024 / 1024 < 5;
    if (!isLt5M) {
      this.message.error('File must be smaller than 5MB!');
      return false;
    }
    this.fileList.update(list => [...list, file]);
    return false; // Prevent automatic upload
  };
  
  removeFile = (fileToRemove: NzUploadFile): void => {
    this.fileList.update(currentFiles => 
      currentFiles.filter(f => f.uid !== fileToRemove.uid)
    );
  }

  submit(): void {
    if (this.answerForm.invalid) {
      this.answerForm.markAllAsTouched();
      return;
    }
    this.isSubmitting.set(true);
    const filesToUpload = this.fileList().map(f => f.originFileObj as File);
    
    this.answerSubmit.emit({
      body: this.answerForm.value.body!,
      files: filesToUpload,
    });
    
    this.answerForm.reset();
    this.fileList.set([]);
    this.isSubmitting.set(false);
  }
}