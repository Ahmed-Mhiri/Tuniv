import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { UserService } from '../../services/user.service';
import { FileUploadService } from '../../../../core/services/file-upload.service';
import { UserProfile, UserProfileUpdateRequest } from '../../../../shared/models/user.model';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';
import { FileUploadComponent } from '../../../../shared/components/file-upload/file-upload.component';
import { AuthService } from '../../../../core/services/auth.service';
import { AuthResponse } from '../../../../shared/models/auth.model';

// NG-ZORRO Imports
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { NzUploadModule } from 'ng-zorro-antd/upload';

@Component({
  selector: 'app-profile-edit-page',
  imports: [
    ReactiveFormsModule,
    SpinnerComponent,
    FileUploadComponent,
    NzFormModule,
    NzInputModule,
    NzButtonModule,
    NzAvatarModule,
    NzDividerModule,
    NzTypographyModule,
    NzUploadModule,
  ],
  templateUrl: './profile-edit-page.component.html',
  styleUrl: './profile-edit-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileEditPageComponent implements OnInit {
  // --- Dependencies ---
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly fileUploadService = inject(FileUploadService);
  private readonly message = inject(NzMessageService);
  private readonly authService = inject(AuthService);

  // --- State Signals ---
  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly isUploading = signal(false);
  readonly profilePhotoUrl = signal<string | undefined>(undefined);

  readonly editForm = this.fb.group({
    bio: ['', [Validators.maxLength(500)]],
    major: ['', [Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.userService.getCurrentUserProfile().subscribe({
      next: (profile) => {
        this.editForm.patchValue(profile);
        this.profilePhotoUrl.set(profile.profilePhotoUrl ?? undefined);
        this.isLoading.set(false);
      },
      error: () => {
        this.message.error('Could not load your profile data.');
        this.isLoading.set(false);
      },
    });
  }

  onProfilePhotoChanged(files: File[]): void {
    const file = files[0];
    if (!file) return;

    this.isUploading.set(true);
    this.fileUploadService.uploadFile(file).subscribe({
      next: (response) => {
        const newPhotoUrl = response.url;
        this.userService.updateCurrentUserProfile({ profilePhotoUrl: newPhotoUrl }).subscribe({
          next: (updatedProfile: UserProfile) => {
            // --- FIX: Update the global user state in AuthService ---
            this.authService.updateCurrentUser(updatedProfile as Partial<AuthResponse>);
            
            this.profilePhotoUrl.set(newPhotoUrl);
            this.isUploading.set(false);
            this.message.success('Profile photo updated successfully!');
          },
          error: () => {
            this.isUploading.set(false);
            this.message.error('Failed to save the new profile photo.');
          },
        });
      },
      error: (err: HttpErrorResponse) => {
        this.isUploading.set(false);
        this.message.error(err.error?.message || 'File upload failed.');
      },
    });
  }

  submitForm(): void {
    if (this.editForm.invalid) return;

    this.isSaving.set(true);
    const { bio, major } = this.editForm.getRawValue();
    const updateData: UserProfileUpdateRequest = {
      bio: bio ?? undefined,
      major: major ?? undefined,
    };

    this.userService.updateCurrentUserProfile(updateData).subscribe({
      next: (updatedProfile: UserProfile) => {
        // --- FIX: Update the global user state in AuthService ---
        this.authService.updateCurrentUser(updatedProfile as Partial<AuthResponse>);

        this.isSaving.set(false);
        this.message.success('Profile details updated successfully!');
        this.editForm.markAsPristine();
      },
      error: (err: HttpErrorResponse) => {
        this.isSaving.set(false);
        this.message.error(err.error?.message || 'Failed to update profile.');
      },
    });
  }
}