import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
  Renderer2,
  PLATFORM_ID
} from '@angular/core';
import {
  DOCUMENT,
  isPlatformBrowser,
  NgOptimizedImage
} from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { FormsModule } from '@angular/forms';

// NG-ZORRO Modules
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';
import { NzDividerModule } from 'ng-zorro-antd/divider';

// Services
import { AuthService } from '../../../core/services/auth.service';
import { SearchBarComponent } from '../search-bar/search-bar';
import { NzBadgeComponent } from 'ng-zorro-antd/badge';
import { ChatWidgetService } from '../../../features/chat/services/chat-widget.service';
import { NotificationDropdown } from '../../../features/notification/components/notification-dropdown/notification-dropdown';
import { NotificationService } from '../../../features/notification/services/notification.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    RouterLinkActive,
    RouterLink,
    FormsModule,
    NzLayoutModule,
    NzMenuModule,
    NzButtonModule,
    NzDropDownModule,
    NzAvatarModule,
    NzIconModule,
    NzSwitchModule,
    NzDrawerModule,
    NzDividerModule,
    SearchBarComponent,
    NotificationDropdown
  ],
  templateUrl: './navbar.component.html',
  // --- THIS LINE WAS MISSING ---
  styleUrls: ['./navbar.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'block'
  }
})
export class NavbarComponent implements OnInit {
  // --- Dependencies ---
  private readonly authService = inject(AuthService);
  private readonly renderer = inject(Renderer2);
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);
  private readonly chatWidgetService = inject(ChatWidgetService); // ✅ 2. Inject the service
  private readonly notificationService = inject(NotificationService); // ✅ 3. Inject the NotificationService



  // --- State Signals ---
  readonly isDarkMode = signal(false);
  readonly isMobileDrawerVisible = signal(false);
  readonly isBrowser = signal(false);
  // ✅ 4. Replace the placeholder with the real signal from the service
  readonly unreadNotificationCount = this.notificationService.unreadNotificationsCount;

  // --- Computed Signals ---
  readonly currentUser = computed(() => this.authService.currentUser());
  readonly isUserLoggedIn = computed(() => this.authService.isUserLoggedIn());
  readonly menuTheme = computed(() => (this.isDarkMode() ? 'dark' : 'light'));

  ngOnInit(): void {
    this.isBrowser.set(isPlatformBrowser(this.platformId));
    if (this.isBrowser()) {
      const savedTheme = localStorage.getItem('theme');
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      const shouldUseDarkMode = savedTheme === 'dark' || (!savedTheme && prefersDark);
      if (shouldUseDarkMode) {
        this.isDarkMode.set(true);
        this.renderer.addClass(this.document.documentElement, 'dark');
      }
    }
  }

  toggleTheme(isDark: boolean): void {
    this.isDarkMode.set(isDark);
    const htmlEl = this.document.documentElement;
    if (isDark) {
      this.renderer.addClass(htmlEl, 'dark');
      localStorage.setItem('theme', 'dark');
    } else {
      this.renderer.removeClass(htmlEl, 'dark');
      localStorage.setItem('theme', 'light');
    }
  }

  openMobileDrawer(): void {
    this.isMobileDrawerVisible.set(true);
  }

  closeMobileDrawer(): void {
    this.isMobileDrawerVisible.set(false);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
  toggleChatWidget(): void {
    this.chatWidgetService.toggleWidget();
  }
}