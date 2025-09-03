import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { of, switchMap } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService } from '../../services/user.service';
import { AuthResponse } from '../../../../shared/models/auth.model';
import { UserProfile } from '../../../../shared/models/user.model';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';
import { CommonModule } from '@angular/common'; // <-- ADDED: Import CommonModule for pipes and directives

// --- ADDED: Import the activity model ---
import { UserActivityItem } from '../../../../shared/models/activity.model';

// NG-ZORRO Imports
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTabsModule } from 'ng-zorro-antd/tabs';

@Component({
  selector: 'app-profile-page',
  standalone: true, // <-- Make sure it's standalone if using new imports structure
  imports: [
    CommonModule, // <-- ADDED: For the date pipe and @if/@for blocks
    RouterLink,
    SpinnerComponent,
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

  // --- Existing Signals ---
  readonly userProfile = signal<UserProfile | AuthResponse | null>(null);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);
  
  // --- ADDED: Signals for the activity feed ---
  readonly activityItems = signal<UserActivityItem[]>([]);
  readonly isActivityLoading = signal(false);
  readonly hasLoadedActivity = signal(false); // To prevent re-fetching

  readonly currentUser = this.authService.currentUser;
  readonly isOwnProfile = computed(() => {
    const currentUserId = this.currentUser()?.userId;
    const profileUserId = (this.userProfile() as UserProfile)?.userId;
    return currentUserId && profileUserId && currentUserId === currentUserId;
  });

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          this.isLoading.set(true);
          const userId = Number(params.get('id'));

          if (this.currentUser()?.userId === userId) {
            this.userProfile.set(this.currentUser());
            this.isLoading.set(false);
            return of(null);
          }

          if (isNaN(userId)) {
            this.error.set('Invalid user ID provided.');
            this.isLoading.set(false);
            return of(null);
          }
          
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

  // --- ADDED: Method to load activity data on demand ---
  loadActivity(): void {
    const profile = this.userProfile();
    // Only fetch if we have a profile ID and haven't fetched before
    if (!profile || this.hasLoadedActivity()) {
      return;
    }

    this.isActivityLoading.set(true);
    this.hasLoadedActivity.set(true);

    this.userService.getUserActivity(profile.userId).subscribe({
      next: (items) => {
        this.activityItems.set(items);
        this.isActivityLoading.set(false);
      },
      error: () => {
        // Optional: Set an error signal for the activity tab specifically
        this.isActivityLoading.set(false);
      },
    });
  }
}