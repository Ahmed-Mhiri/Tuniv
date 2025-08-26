import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable, switchMap } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';

// NG-ZORRO Imports
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { UserService } from '../../services/user.service';
import { UserProfile } from '../../../../shared/models/user.model';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';

@Component({
  selector: 'app-profile-page',
  imports: [
    RouterLink,
    NzSpinModule,
    NzAlertModule,
    NzCardModule,
    NzAvatarModule,
    NzButtonModule,
    NzIconModule,
    NzTabsModule,
    SpinnerComponent
  ],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePageComponent implements OnInit {
  // --- Dependencies ---
  private readonly route = inject(ActivatedRoute);
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);

  // --- State Signals ---
  readonly userProfile = signal<UserProfile | null>(null);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);
  
  // --- Computed Signals ---
  readonly currentUser = this.authService.currentUser;
  readonly isOwnProfile = computed(() => {
    return this.currentUser()?.id === this.userProfile()?.userId;
  });

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          this.isLoading.set(true);
          const userId = Number(params.get('id'));

          // --- FIX: Add a check for NaN ---
          if (isNaN(userId)) {
            this.error.set('Invalid user ID provided.');
            this.isLoading.set(false);
            // Return an empty observable to stop the chain
            return new Observable<UserProfile | null>(subscriber => subscriber.complete());
          }

          return this.userService.getUserProfileById(userId);
        })
      )
      .subscribe({
        next: (profile) => {
          if (profile) { // Check if the observable emitted a value
            this.userProfile.set(profile);
          }
          this.isLoading.set(false);
        },
        error: () => {
          this.error.set('Could not load user profile. The user may not exist.');
          this.isLoading.set(false);
        },
      });
  }
}