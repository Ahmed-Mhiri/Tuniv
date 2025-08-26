import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

// NG-ZORRO Imports
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMessageService } from 'ng-zorro-antd/message'; // For success feedback

// Custom validator to check if passwords match
export const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');
  
  // Return null if controls haven't been initialized yet, or if they match
  if (!password || !confirmPassword || password.value === confirmPassword.value) {
    return null;
  }
  
  // Return an error object if they don't match
  return { passwordsMismatch: true };
};

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink,
    NzFormModule, NzInputModule, NzButtonModule,
    NzAlertModule, NzIconModule
  ],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterPageComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private message = inject(NzMessageService); // NG-ZORRO's toast message service

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  passwordVisible = signal(false);
  confirmPasswordVisible = signal(false);

  registerForm: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(40)]],
    confirmPassword: ['', [Validators.required]],
  }, { validators: passwordMatchValidator }); // Apply the custom validator to the whole form

  togglePasswordVisibility(): void {
    this.passwordVisible.update(value => !value);
  }

  toggleConfirmPasswordVisibility(): void {
    this.confirmPasswordVisible.update(value => !value);
  }

  submitForm(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    // We don't need to send 'confirmPassword' to the backend
    const { username, email, password } = this.registerForm.getRawValue();

    this.authService.register({ username, email, password }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.message.success('Registration successful! Please log in.');
        this.router.navigate(['/login']);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        // The backend error message is already user-friendly
        const message = err.error || 'An error occurred during registration.';
        this.errorMessage.set(message);
      },
    });
  }
}