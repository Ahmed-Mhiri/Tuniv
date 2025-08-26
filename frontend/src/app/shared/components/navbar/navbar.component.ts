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
  // Dependency injection
  private authService = inject(AuthService);
  private renderer = inject(Renderer2);
  private document = inject(DOCUMENT);
  private platformId = inject(PLATFORM_ID);
  private router = inject(Router);

  // State signals
  isDarkMode = signal(false);
  isMobileDrawerVisible = signal(false);
  isBrowser = signal(false);

  // Computed properties
  currentUser = computed(() => this.authService.currentUser());
  isUserLoggedIn = computed(() => this.authService.isUserLoggedIn());
  menuTheme = computed(() => this.isDarkMode() ? 'dark' : 'light');

  ngOnInit(): void {
    // Check if we're in the browser environment
    this.isBrowser.set(isPlatformBrowser(this.platformId));

    if (this.isBrowser()) {
      const savedTheme = localStorage.getItem('theme');
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

      // Set theme based on saved preference or system preference
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
}