import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../../core/services/auth.service';

// NG-ZORRO Imports
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzIconModule } from 'ng-zorro-antd/icon';

@Component({
  selector: 'app-forgot-password-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    NzFormModule,
    NzInputModule,
    NzButtonModule,
    NzAlertModule,
    NzIconModule,
  ],
  templateUrl: './forgot-password-page.html',
  styleUrl: './forgot-password-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForgotPasswordPageComponent {
  // --- Dependencies ---
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  // --- State Signals ---
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  // --- Form Definition ---
  readonly forgotPasswordForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  // --- Logic ---
    submitForm(): void {
    if (this.forgotPasswordForm.invalid) {
      this.forgotPasswordForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const { email } = this.forgotPasswordForm.getRawValue();

    // --- FIX: Ensure email is not null before calling the service ---
    if (email) {
      this.authService.forgotPassword({ email }).subscribe({
        next: (response: any) => {
          this.isLoading.set(false);
          this.successMessage.set(
            response ||
              'If an account exists for this email, a reset link has been sent.'
          );
          this.forgotPasswordForm.reset();
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.errorMessage.set(
            err.error || 'An error occurred. Please try again.'
          );
        },
      });
    } else {
      // This case should not happen due to the validation guard above,
      // but it makes the code robust.
      this.isLoading.set(false);
      this.errorMessage.set('Email is missing. Please try again.');
    }
  }
}