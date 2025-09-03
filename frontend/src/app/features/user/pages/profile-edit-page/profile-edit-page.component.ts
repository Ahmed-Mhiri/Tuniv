// profile-edit-page.component.ts

import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { UserService } from '../../services/user.service';
import { FileUploadService } from '../../../../core/services/file-upload.service';
import { UserProfile, UserProfileUpdateRequest } from '../../../../shared/models/user.model';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';
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
import { NzIconModule } from 'ng-zorro-antd/icon'; // <-- Import NzIconModule

@Component({
  selector: 'app-profile-edit-page',
  standalone: true, // <-- Make sure it's standalone if using new imports structure
  imports: [
    ReactiveFormsModule,
    SpinnerComponent,
    // FileUploadComponent is no longer needed here
    NzFormModule,
    NzInputModule,
    NzButtonModule,
    NzAvatarModule,
    NzDividerModule,
    NzTypographyModule,
    NzIconModule, // <-- Add NzIconModule to imports
  ],
  templateUrl: './profile-edit-page.component.html',
  styleUrl: './profile-edit-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileEditPageComponent implements OnInit, OnDestroy {
  // --- Dependencies ---
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly fileUploadService = inject(FileUploadService);
  private readonly message = inject(NzMessageService);
  private readonly authService = inject(AuthService);

  // --- State Signals ---
  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  
  // -- Photo State --
  readonly profilePhotoUrl = signal<string | undefined>(undefined);
  readonly newProfilePhotoFile = signal<File | null>(null);
  readonly newProfilePhotoPreviewUrl = signal<string | null>(null);

  // -- Computed signal to decide which URL to display --
  readonly displayPhotoUrl = computed(() => this.newProfilePhotoPreviewUrl() ?? this.profilePhotoUrl());

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

  ngOnDestroy(): void {
    // Clean up the object URL to prevent memory leaks
    const previewUrl = this.newProfilePhotoPreviewUrl();
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
  }

  onProfilePhotoChanged(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    // Validate file type and size before creating a preview
    if (!['image/png', 'image/jpeg', 'image/gif'].includes(file.type)) {
        this.message.error('Invalid file type. Please select a PNG, JPG, or GIF.');
        return;
    }
    if (file.size > 5 * 1024 * 1024) { // 5MB
        this.message.error('File is too large. Maximum size is 5MB.');
        return;
    }

    // Store the file for later upload
    this.newProfilePhotoFile.set(file);

    // Create a local URL for previewing the image
    const oldPreviewUrl = this.newProfilePhotoPreviewUrl();
    if (oldPreviewUrl) {
        URL.revokeObjectURL(oldPreviewUrl); // Clean up previous preview
    }
    this.newProfilePhotoPreviewUrl.set(URL.createObjectURL(file));

    // Mark the form as dirty to enable the save button
    this.editForm.markAsDirty();
  }

  submitForm(): void {
    if (this.editForm.invalid) return;

    this.isSaving.set(true);
    const newFile = this.newProfilePhotoFile();

    const uploadAndSave$ = (newFile ? this.fileUploadService.uploadFile(newFile) : of({ url: this.profilePhotoUrl() }));

    uploadAndSave$.pipe(
      switchMap(uploadResponse => {
        const { bio, major } = this.editForm.getRawValue();
        const updateData: UserProfileUpdateRequest = {
          bio: bio ?? undefined,
          major: major ?? undefined,
          profilePhotoUrl: uploadResponse?.url
        };
        return this.userService.updateCurrentUserProfile(updateData);
      })
    ).subscribe({
      next: (updatedProfile: UserProfile) => {
        this.authService.updateCurrentUser(updatedProfile as Partial<AuthResponse>);
        this.profilePhotoUrl.set(updatedProfile.profilePhotoUrl ?? undefined);
        
        // Reset state after successful save
        this.newProfilePhotoFile.set(null);
        this.newProfilePhotoPreviewUrl.set(null);
        this.editForm.markAsPristine();
        
        this.isSaving.set(false);
        this.message.success('Profile updated successfully!');
      },
      error: (err: HttpErrorResponse) => {
        this.isSaving.set(false);
        this.message.error(err.error?.message || 'Failed to update profile.');
      },
    });
  }
}