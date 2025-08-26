import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { first } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';

// NG-ZORRO Imports
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMessageService } from 'ng-zorro-antd/message';

// Custom validator to check if passwords match
export const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');
  return !password || !confirmPassword || password.value === confirmPassword.value ? null : { passwordsMismatch: true };
};

@Component({
  selector: 'app-reset-password-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    NzFormModule,
    NzInputModule,
    NzButtonModule,
    NzAlertModule,
    NzIconModule,
  ],
  templateUrl: './reset-password-page.html',
  styleUrl: './reset-password-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResetPasswordPageComponent implements OnInit {
  // --- Dependencies ---
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly message = inject(NzMessageService);

  // --- State Signals ---
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly token = signal<string | null>(null);
  readonly passwordVisible = signal(false);
  readonly confirmPasswordVisible = signal(false);

  // --- Form Definition ---
  readonly resetPasswordForm = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]],
  }, { validators: passwordMatchValidator });

  ngOnInit(): void {
    this.route.queryParamMap.pipe(first()).subscribe(params => {
      const tokenFromUrl = params.get('token');
      if (tokenFromUrl) {
        this.token.set(tokenFromUrl);
      } else {
        this.errorMessage.set('No reset token found. The link may be invalid or expired.');
      }
    });
  }

  // --- Logic ---
  submitForm(): void {
    if (this.resetPasswordForm.invalid) {
      this.resetPasswordForm.markAllAsTouched();
      return;
    }
    const currentToken = this.token();
    if (!currentToken) {
      this.errorMessage.set('Cannot reset password without a valid token.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    // This returns { password: string | null }
    const { password } = this.resetPasswordForm.getRawValue();

    // --- FIX: Add a type guard to ensure 'password' is a string ---
    if (password) {
      this.authService.resetPassword({ token: currentToken, newPassword: password }).subscribe({
        next: () => {
          this.isLoading.set(false);
          this.message.success('Your password has been reset successfully!');
          this.router.navigate(['/login']);
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.errorMessage.set(err.error || 'An error occurred. The token may be invalid or expired.');
        },
      });
    } else {
      // This is a fallback, as form validation should prevent this
      this.isLoading.set(false);
      this.errorMessage.set('Password cannot be empty.');
    }
  }
}