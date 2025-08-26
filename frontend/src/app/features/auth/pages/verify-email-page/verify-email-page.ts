import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { first } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';

// NG-ZORRO Imports
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';

@Component({
  selector: 'app-verify-email-page',
  imports: [RouterLink, NzSpinModule, NzAlertModule, NzButtonModule],
  templateUrl: './verify-email-page.html',
  styleUrl: './verify-email-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerifyEmailPageComponent implements OnInit {
  // --- Dependencies ---
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  // --- State Signals ---
  readonly isLoading = signal(true); // Start in a loading state
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.route.queryParamMap.pipe(first()).subscribe((params) => {
      const token = params.get('token');

      if (token) {
        this.authService.verify(token).subscribe({
          next: () => {
            this.successMessage.set('Your account has been verified successfully!');
            this.isLoading.set(false);
          },
          error: (err: HttpErrorResponse) => {
            this.errorMessage.set(
              err.error?.error ||
                'Verification failed. The link may be invalid or expired.'
            );
            this.isLoading.set(false);
          },
        });
      } else {
        this.errorMessage.set('No verification token provided in the URL.');
        this.isLoading.set(false);
      }
    });
  }
}