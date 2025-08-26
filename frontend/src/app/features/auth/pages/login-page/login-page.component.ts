import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthRequest } from '../../../../shared/models/auth.model'; // <-- Import AuthRequest

// NG-ZORRO Imports
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzIconModule } from 'ng-zorro-antd/icon';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink,
    NzFormModule, NzInputModule, NzButtonModule,
    NzCheckboxModule, NzAlertModule, NzIconModule
  ],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPageComponent {
  // Inject services and dependencies
  private fb = inject(FormBuilder);
  private router = inject(Router);
  // Make the service public to access its signals in the template
  public authService = inject(AuthService);

  // Component state signals
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  passwordVisible = signal(false);

  // --- MODIFIED: Add a 'code' control for 2FA ---
  loginForm: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    remember: [true],
    code: ['', [Validators.minLength(6), Validators.maxLength(6)]], // For the 6-digit 2FA code
  });

  togglePasswordVisibility(): void {
    this.passwordVisible.update(value => !value);
  }

  // --- NEW: Method to go back from 2FA step ---
  cancelTwoFactorAuth(): void {
    this.authService.isTwoFactorStep.set(false);
    this.errorMessage.set(null);
    this.loginForm.get('code')?.reset();
  }

  // --- MODIFIED: Handle both login steps ---
  submitForm(): void {
    const isTwoFactorStep = this.authService.isTwoFactorStep();
    
    // Validate the correct part of the form based on the current step
    if ((!isTwoFactorStep && (this.loginForm.get('username')?.invalid || this.loginForm.get('password')?.invalid)) ||
        (isTwoFactorStep && this.loginForm.get('code')?.invalid)) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const credentials: AuthRequest = this.loginForm.getRawValue();

    this.authService.login(credentials).subscribe({
      next: (response) => {
        // If the login is fully successful (not a 2FA step), navigate home
        if (!response.is2faRequired) {
          this.isLoading.set(false);
          this.router.navigate(['/']);
        } else {
          // If 2FA is required, the service sets the state. Just stop the loading spinner.
          this.isLoading.set(false);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        const message = err.error || 'An unexpected error occurred. Please try again.';
        this.errorMessage.set(message);
      },
    });
  }
}