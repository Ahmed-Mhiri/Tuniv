import { ChangeDetectionStrategy, Component, effect, input, output, signal } from '@angular/core';
import { NzUploadFile, NzUploadModule } from 'ng-zorro-antd/upload';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzIconModule } from 'ng-zorro-antd/icon';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [NzUploadModule, NzIconModule],
  templateUrl: './file-upload.component.html',
  styleUrl: './file-upload.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FileUploadComponent {
  // --- INPUTS ---
  // A signal to hold the list of files, enabling two-way binding.
  fileList = signal<NzUploadFile[]>([]);
  
  // New: Inputs to make the component highly reusable.
  acceptedTypes = input<string[]>(['image/png', 'image/jpeg', 'image/gif']);
  maxFileSize = input<number>(2); // Max size in MB
  multiple = input<boolean>(true);
  uploadText = input<string>('Click or drag files to this area to upload');
  uploadHint = input<string>('Support for a single or bulk upload.');

  // --- OUTPUTS ---
  // This output now emits the list whenever it changes.
  filesChanged = output<File[]>();

  constructor(private msg: NzMessageService) {
    // This effect runs whenever 'fileList' changes, automatically emitting the new list.
    effect(() => {
      const files = this.fileList()
        .map(f => f.originFileObj as File)
        .filter(f => f); // Filter out any undefined files
      this.filesChanged.emit(files);
      console.log('File list changed, emitted:', files);
    });
  }

  // This method now ONLY handles validation before a file is added.
  beforeUpload = (file: NzUploadFile): boolean => {
    // 1. Validate file type
    const hasValidType = this.acceptedTypes().includes(file.type!);
    if (!hasValidType) {
      this.msg.error(`Invalid file type. Please upload one of: ${this.acceptedTypes().join(', ')}`);
      return false;
    }

    // 2. Validate file size
    const isLt2M = file.size! / 1024 / 1024 < this.maxFileSize();
    if (!isLt2M) {
      this.msg.error(`File must be smaller than ${this.maxFileSize()}MB!`);
      return false;
    }

    // If all checks pass, allow the file to be added to the list.
    // We return true and let nz-upload handle adding it to our signal.
    return true;
  };
}