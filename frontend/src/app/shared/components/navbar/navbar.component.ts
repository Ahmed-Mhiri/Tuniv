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
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';

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
    NotificationDropdown,
    NzToolTipModule,
    SearchBarComponent,
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
  private readonly platformId = inject(PLATFORM_ID); // 👈 2. Use the correct token here
  private readonly router = inject(Router);
  private readonly chatWidgetService = inject(ChatWidgetService);
  private readonly notificationService = inject(NotificationService);

  // --- State Signals ---
  readonly isDarkMode = signal(false);
  readonly isMobileDrawerVisible = signal(false);
  readonly isBrowser = signal(false);
  /**
   * ✅ NEW: Controls the visual state of the navbar when the user scrolls.
   */
  readonly isScrolled = signal(false);
  /**
   * ✅ NEW: Dynamic placeholder text for the search bar.
   */
  readonly searchPlaceholder = signal('Search for universities, courses, and more');
  
  // --- Service-driven State ---
  readonly unreadNotificationCount = this.notificationService.unreadNotificationsCount;
  /**
   * ✅ NEW: Tracks unread chat messages from the service.
   * Assumes ChatWidgetService exposes a signal named `unreadCount`.
   */
  readonly unreadMessageCount = this.chatWidgetService.unreadCount;

  // --- Computed Signals ---
  readonly currentUser = computed(() => this.authService.currentUser());
  readonly isUserLoggedIn = computed(() => this.authService.isUserLoggedIn());
  readonly menuTheme = computed(() => (this.isDarkMode() ? 'dark' : 'light'));
  /**
   * ✅ NEW: Determines if the unread message indicator should be shown.
   */
  readonly hasUnreadMessages = computed(() => this.unreadMessageCount() > 0);

  ngOnInit(): void {
    this.isBrowser.set(isPlatformBrowser(this.platformId));
    if (this.isBrowser()) {
      // Initialize theme based on user preference or system settings
      const savedTheme = localStorage.getItem('theme');
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      const shouldUseDarkMode = savedTheme === 'dark' || (!savedTheme && prefersDark);
      
      this.updateTheme(shouldUseDarkMode);

      // Check initial scroll position
      this.onWindowScroll();
    }
  }

  /**
   * ✅ NEW: Event handler for the window's scroll event.
   * Updates the `isScrolled` signal based on the vertical scroll position.
   */
  onWindowScroll(): void {
    if (this.isBrowser()) {
      const scrollY = this.document.defaultView?.scrollY ?? 0;
      this.isScrolled.set(scrollY > 50);
    }
  }

  /**
   * Toggles the application's theme between dark and light mode.
   * @param isDark A boolean indicating whether to switch to dark mode.
   */
  toggleTheme(isDark: boolean): void {
    this.isDarkMode.set(isDark);
    this.updateTheme(isDark);
  }

  /**
   * Helper function to apply theme class and save preference.
   */
  private updateTheme(isDark: boolean): void {
    const htmlEl = this.document.documentElement;
    if (isDark) {
      this.renderer.addClass(htmlEl, 'dark');
      localStorage.setItem('theme', 'dark');
    } else {
      this.renderer.removeClass(htmlEl, 'dark');
      localStorage.setItem('theme', 'light');
    }
    this.isDarkMode.set(isDark);
  }

  openMobileDrawer(): void {
    this.isMobileDrawerVisible.set(true);
  }

  closeMobileDrawer(): void {
    this.isMobileDrawerVisible.set(false);
  }

  /**
   * ✅ NEW: Navigates to a given route and closes the mobile drawer.
   * Used by the mobile menu items for a smoother user experience.
   * @param route The destination route (string or string array).
   */
  navigateAndClose(route: string | any[]): void {
    // Ensure route is an array for the navigate method
    const commands = Array.isArray(route) ? route : [route];
    this.router.navigate(commands);
    this.closeMobileDrawer();
  }
  
  /**
   * ✅ NEW: A placeholder for handling clicks on desktop navigation links.
   * This can be expanded to add functionality like scrolling to the top
   * if the user clicks on the link for the page they are already on.
   */
  handleNavClick(event: MouseEvent): void {
    // Currently, this does nothing extra. The routerLink handles navigation.
    // You could add logic here if needed.
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }

  toggleChatWidget(): void {
    this.chatWidgetService.toggleWidget();
  }
}
