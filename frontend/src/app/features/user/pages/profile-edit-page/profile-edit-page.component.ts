import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
// NG-ZORRO Imports
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { UserService } from '../../services/user.service';
import { UserProfileUpdateRequest } from '../../../../shared/models/user.model';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';

@Component({
  selector: 'app-profile-edit-page',
  imports: [
    ReactiveFormsModule,
    NzFormModule,
    NzInputModule,
    NzButtonModule,
    NzSpinModule,
    SpinnerComponent
  ],
  templateUrl: './profile-edit-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileEditPageComponent implements OnInit {
  // --- Dependencies ---
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly message = inject(NzMessageService);

  // --- State Signals ---
  readonly isLoading = signal(true); // Start true to load initial data
  readonly isSaving = signal(false);

  // --- Form Definition ---
  readonly editForm = this.fb.group({
    bio: ['', [Validators.maxLength(500)]],
    major: ['', [Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    // Fetch the current user's profile to populate the form
    this.userService.getCurrentUserProfile().subscribe({
      next: (profile) => {
        this.editForm.patchValue(profile);
        this.isLoading.set(false);
      },
      error: () => {
        this.message.error('Could not load your profile data.');
        this.isLoading.set(false);
      },
    });
  }

  submitForm(): void {
    if (this.editForm.invalid) {
      return;
    }

    this.isSaving.set(true);
    
    // --- FIX: Manually construct the update object ---
    const { bio, major } = this.editForm.getRawValue();
    const updateData: UserProfileUpdateRequest = {
      bio: bio ?? undefined,
      major: major ?? undefined
    };

    this.userService.updateCurrentUserProfile(updateData).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.message.success('Profile updated successfully!');
        this.editForm.markAsPristine(); // Mark form as not dirty after saving
      },
      error: (err: HttpErrorResponse) => {
        this.isSaving.set(false);
        this.message.error(err.error?.message || 'Failed to update profile.');
      },
    });
  }
}