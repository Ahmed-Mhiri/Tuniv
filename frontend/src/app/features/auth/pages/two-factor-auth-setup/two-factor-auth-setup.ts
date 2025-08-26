import { ChangeDetectionStrategy, Component, OnInit, computed, inject, model, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../../core/services/auth.service';

// NG-ZORRO Imports
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { AuthResponse } from '../../../../shared/models/auth.model';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';

@Component({
  selector: 'app-two-factor-auth-setup',
  imports: [
    FormsModule,
    NzButtonModule,
    NzAlertModule,
    NzInputModule,
    NzSpinModule,
    NzDividerModule,
    NzTypographyModule,
    SpinnerComponent
  ],
  templateUrl: './two-factor-auth-setup.html',
  styleUrl: './two-factor-auth-setup.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TwoFactorAuthSetupComponent {
  // --- Dependencies ---
  private readonly authService = inject(AuthService);
  private readonly message = inject(NzMessageService);

  // --- State Signals ---
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly qrCodeUri = signal<string | null>(null);
  readonly verificationCode = model('');

  // --- Computed Signal to check user's 2FA status from the AuthService ---
  readonly is2faEnabled = computed(
    () => this.authService.currentUser()?.is2faEnabled ?? false
  );

  generateSecret(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.authService.generate2faSecret().subscribe({
      next: (response) => {
        this.qrCodeUri.set(response.qrCodeUri);
        this.isLoading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          err.error?.message || 'Failed to generate a 2FA secret.'
        );
        this.isLoading.set(false);
      },
    });
  }

  enable2fa(): void {
    if (this.verificationCode().length !== 6) {
      this.errorMessage.set('Please enter a valid 6-digit code.');
      return;
    }
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.authService.enable2fa(this.verificationCode()).subscribe({
      next: (response: AuthResponse) => {
        this.authService.currentUser.set(response); // Update global state
        this.isLoading.set(false);
        this.message.success('Two-Factor Authentication enabled successfully!');
        this.qrCodeUri.set(null);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err.error || 'The verification code is incorrect.'
        );
      },
    });
  }

  disable2fa(): void {
    this.isLoading.set(true);
    this.authService.disable2fa().subscribe({
      next: (response: AuthResponse) => {
        this.authService.currentUser.set(response); // Update global state
        this.isLoading.set(false);
        this.message.success('Two-Factor Authentication has been disabled.');
      },
      error: () => {
        this.isLoading.set(false);
        this.message.error('Failed to disable 2FA. Please try again.');
      },
    });
  }
}