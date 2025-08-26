import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { of, switchMap } from 'rxjs'; // <-- Import 'of'
import { AuthService } from '../../../../core/services/auth.service';
import { UserService } from '../../services/user.service';
import { AuthResponse } from '../../../../shared/models/auth.model';
import { UserProfile } from '../../../../shared/models/user.model';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';

// NG-ZORRO Imports
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTabsModule } from 'ng-zorro-antd/tabs';

@Component({
  selector: 'app-profile-page',
  imports: [
    RouterLink,
    SpinnerComponent,
    NzSpinModule,
    NzAlertModule,
    NzCardModule,
    NzAvatarModule,
    NzButtonModule,
    NzIconModule,
    NzTabsModule,
  ],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);

  readonly userProfile = signal<UserProfile | AuthResponse | null>(null);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);
  
  readonly currentUser = this.authService.currentUser;
  readonly isOwnProfile = computed(() => {
    const currentUserId = this.currentUser()?.userId;
    // The userProfile signal can now hold either type, so we check userId on both
    const profileUserId = (this.userProfile() as UserProfile)?.userId;
    return currentUserId && profileUserId && currentUserId === profileUserId;
  });

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          this.isLoading.set(true);
          const userId = Number(params.get('id'));

          // --- FIX: Check against 'userId' instead of 'id' ---
          if (this.currentUser()?.userId === userId) {
            // If it's our own profile, use the reactive signal from AuthService
            this.userProfile.set(this.currentUser());
            this.isLoading.set(false);
            return of(null); // Stop the observable chain
          }

          if (isNaN(userId)) {
            this.error.set('Invalid user ID provided.');
            this.isLoading.set(false);
            return of(null);
          }
          
          // If it's someone else's profile, fetch it from the API
          return this.userService.getUserProfileById(userId);
        })
      )
      .subscribe({
        next: (profile) => {
          if (profile) {
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