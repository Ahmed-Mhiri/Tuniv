import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Attachment } from '../../../../shared/models/qa.model';

// NG-ZORRO Module Imports
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzUploadModule, NzUploadFile } from 'ng-zorro-antd/upload';
import { NzIconModule } from 'ng-zorro-antd/icon';

export interface PostEditSaveEvent {
  body: string;
  title?: string;
  newFiles: File[];
  attachmentIdsToDelete: number[];
}

@Component({
  selector: 'app-post-edit-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    NzFormModule,
    NzInputModule,
    NzButtonModule,
    NzUploadModule,
    NzIconModule
  ],
  templateUrl: './post-edit-form.component.html',
  styleUrls: ['./post-edit-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PostEditFormComponent implements OnInit {
  // --- Inputs & Outputs ---
  initialTitle = input<string>();
  initialBody = input.required<string>();
  initialAttachments = input<Attachment[]>([]);
  postType = input<'Question' | 'Answer' | 'Comment'>('Answer');
  save = output<PostEditSaveEvent>();
  cancel = output<void>();
  
  private readonly fb = inject(FormBuilder);

  // Initialize as undefined and create in ngOnInit
  editForm!: FormGroup;

  existingAttachments = signal<Attachment[]>([]);
  attachmentIdsToDelete = signal<number[]>([]);
  newFiles = signal<File[]>([]);
  newFileList = signal<NzUploadFile[]>([]);
  
  readonly bodyErrorTip = computed(() => {
    switch (this.postType()) {
      case 'Question':
        return 'Body is required and must be at least 20 characters.';
      case 'Answer':
        return 'Answer is required and must be at least 15 characters.';
      case 'Comment':
        return 'Comment is required and must be at least 5 characters.';
      default:
        return 'This field is required.';
    }
  });

  private formGroupValidator = (control: AbstractControl): ValidationErrors | null => {
    const bodyControl = control.get('body');
    const bodyValue = bodyControl?.value;
    const hasFiles = this.existingAttachments().length > 0 || this.newFileList().length > 0;
    const isBodyEmpty = !bodyValue || bodyValue.trim().length === 0;

    if (isBodyEmpty && !hasFiles) {
      return { atLeastOneRequired: true };
    }
    if (!isBodyEmpty && bodyControl?.invalid) {
      return { bodyTooShort: true };
    }
    return null;
  };

  ngOnInit(): void {
    // Initialize signals first
    this.existingAttachments.set([...this.initialAttachments()]);
    this.attachmentIdsToDelete.set([]);
    this.newFiles.set([]);
    this.newFileList.set([]);

    // Now create the form after signals are initialized
    this.editForm = this.fb.nonNullable.group({
      title: [''],
      body: [''],
    }, {
      validators: this.formGroupValidator
    });

    if (this.postType() === 'Question') {
      this.editForm.controls['title'].setValidators([Validators.required, Validators.minLength(10)]);
      this.editForm.controls['title'].setValue(this.initialTitle() ?? '');
    }

    let bodyValidators = [];
    switch (this.postType()) {
      case 'Question':
        bodyValidators.push(Validators.minLength(20));
        break;
      case 'Answer':
        bodyValidators.push(Validators.minLength(15));
        break;
      case 'Comment':
        bodyValidators.push(Validators.minLength(5));
        break;
    }
    this.editForm.controls['body'].setValidators(bodyValidators);
    this.editForm.controls['body'].setValue(this.initialBody());
  }

  deleteExistingAttachment(attachmentId: number): void {
    this.existingAttachments.update(atts => atts.filter(a => a.attachmentId !== attachmentId));
    this.attachmentIdsToDelete.update(ids => [...ids, attachmentId]);
  }

  beforeUpload = (file: NzUploadFile): boolean => {
    this.newFiles.update(files => [...files, file as unknown as File]);
    this.newFileList.update(list => [...list, file]);
    return false;
  };
  
  removeNewFile(fileToRemove: NzUploadFile): void {
    this.newFileList.update(list => list.filter(f => f.uid !== fileToRemove.uid));
    this.newFiles.set(this.newFileList().map(f => f.originFileObj as File).filter(Boolean));
  }

  onSave(): void {
    if (this.editForm.invalid) {
      Object.values(this.editForm.controls).forEach(control => control.markAsTouched());
      return;
    }
    this.save.emit({
      title: this.editForm.value.title,
      body: this.editForm.value.body!,
      newFiles: this.newFiles(),
      attachmentIdsToDelete: this.attachmentIdsToDelete(),
    });
  }
}