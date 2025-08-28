import { ChangeDetectionStrategy, Component, output, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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

  readonly answerForm = this.fb.group({
    body: ['', [Validators.required, Validators.minLength(20)]],
  });
  
  handleFileChange({ fileList }: NzUploadChangeParam): void {
    // This method validates and updates the file lists when files are added or removed by the component.
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
  }

  // --- THIS IS THE FIX ---
  // This method is called by the 'x' button on the file preview.
  removeFile(fileToRemove: NzUploadFile): void {
    // We manually trigger the nzChange event by creating a new filtered list,
    // which ensures our handleFileChange logic is the single source of truth.
    const newFileList = this.fileList().filter(f => f.uid !== fileToRemove.uid);
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