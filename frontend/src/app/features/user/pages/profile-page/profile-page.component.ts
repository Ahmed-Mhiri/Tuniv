import { ChangeDetectionStrategy, Component, OnInit, TemplateRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { of, switchMap } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { UserService } from '../../services/user.service';
import { AuthResponse } from '../../../../shared/models/auth.model';
import { UserProfile } from '../../../../shared/models/user.model';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner.component';
import { CommonModule } from '@angular/common';

import { UserActivityItem } from '../../../../shared/models/activity.model';

// NG-ZORRO Imports
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTabsModule, NzTabChangeEvent } from 'ng-zorro-antd/tabs'; // <-- Import NzTabChangeEvent
import { ChatWidgetService } from '../../../chat/services/chat-widget.service';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzModalModule, NzModalRef, NzModalService } from 'ng-zorro-antd/modal'; // <-- ADDED

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    SpinnerComponent,
    NzAlertModule,
    NzCardModule,
    NzAvatarModule,
    NzButtonModule,
    NzIconModule,
    NzTabsModule,
    NzToolTipModule,
    NzModalModule, // <-- ADDED
  ],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePageComponent implements OnInit {
  // --- Dependencies ---
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router); // <-- ADDED
  private readonly userService = inject(UserService);
  public readonly authService = inject(AuthService);
  private readonly chatWidgetService = inject(ChatWidgetService);
  private readonly modal = inject(NzModalService); // <-- ADDED

  // --- State Signals ---
  readonly userProfile = signal<UserProfile | AuthResponse | null>(null);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);
  readonly activityItems = signal<UserActivityItem[]>([]);
  readonly isActivityLoading = signal(false);
  readonly hasLoadedActivity = signal(false);
  private activeModal: NzModalRef | null = null; // <-- ADDED

  readonly currentUser = this.authService.currentUser;

  // --- Computed Signals ---
  readonly isOwnProfile = computed(() => {
    const currentUserId = this.currentUser()?.userId;
    const profileUserId = (this.userProfile() as UserProfile)?.userId;
    return currentUserId && profileUserId && currentUserId === profileUserId;
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

  /**
   * --- MODIFIED & RENAMED ---
   * Now called fetchActivity, containing only the data fetching logic.
   */
  private fetchActivity(): void {
    const profile = this.userProfile() as UserProfile;
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
        // The 403 error will be caught here, but the user won't see it because
        // the modal would have already appeared.
        this.isActivityLoading.set(false);
      },
    });
  }

  startConversation(): void {
    const profile = this.userProfile() as UserProfile;
    if (profile && profile.userId) {
      const participantPayload = {
        participantId: profile.userId,
        participantName: profile.username,
        participantAvatarUrl: profile.profilePhotoUrl,
      };
      this.chatWidgetService.startConversationWithUser(participantPayload);
    }
  }

  // --- ALL NEW METHODS ADDED BELOW ---

  /**
   * Handles tab changes. If the "Activity" tab is selected, it checks
   * for authentication before loading data.
   */
  onTabChange(event: NzTabChangeEvent, modalFooter: TemplateRef<{}>): void {
    // We only care when the "Activity" tab is selected (index 1)
    if (event.index !== 1) {
      return;
    }

    // Check if the user is logged in
    if (!this.authService.isUserLoggedIn()) {
      this.showLoginModal(modalFooter);
      return; // Stop execution to prevent API call
    }

    // If logged in, proceed to fetch the activity data
    this.fetchActivity();
  }

  /**
   * Displays the "Login Required" modal.
   */
  private showLoginModal(modalFooter: TemplateRef<{}>): void {
    this.activeModal = this.modal.create({
      nzTitle: 'Login Required',
      nzContent: 'You must be logged in to view user activity.',
      nzFooter: modalFooter,
    });

    this.activeModal.afterClose.subscribe(() => {
      this.activeModal = null;
    });
  }

  handleCancel(): void {
    this.activeModal?.destroy();
  }

  handleRegister(): void {
    this.activeModal?.destroy();
    this.router.navigate(['/auth/register']);
  }

  handleLogin(): void {
    this.activeModal?.destroy();
    this.router.navigate(['/auth/login']);
  }
}