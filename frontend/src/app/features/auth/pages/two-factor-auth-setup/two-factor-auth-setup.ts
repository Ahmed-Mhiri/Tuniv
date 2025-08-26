import { ChangeDetectionStrategy, Component, OnInit, inject, model, signal } from '@angular/core';
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
  ],
  templateUrl: './two-factor-auth-setup.html',
  styleUrl: './two-factor-auth-setup.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TwoFactorAuthSetupComponent implements OnInit {
  // --- Dependencies ---
  private readonly authService = inject(AuthService);
  private readonly message = inject(NzMessageService);

  // --- State Signals ---
  readonly isLoading = signal(true); // Start in loading state
  readonly errorMessage = signal<string | null>(null);
  readonly qrCodeUri = signal<string | null>(null);

  // Use model() for two-way binding on the verification code input
  readonly verificationCode = model('');

  ngOnInit(): void {
    this.generateSecret();
  }

  // --- Logic ---
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
          err.error?.message || 'Failed to generate a 2FA secret. Please try again.'
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
      next: () => {
        this.isLoading.set(false);
        this.message.success('Two-Factor Authentication has been enabled successfully!');
        // Here you might want to update the user's state or redirect them.
        // For now, we can hide the setup to prevent re-enabling.
        this.qrCodeUri.set(null); 
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err.error || 'The verification code is incorrect. Please try again.'
        );
      },
    });
  }
}