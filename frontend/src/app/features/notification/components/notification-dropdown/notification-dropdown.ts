import { Component, inject } from '@angular/core';
import { NotificationService } from '../../services/notification.service';
import { Router } from '@angular/router';
import { Notification } from '../../../../shared/models/notification.model';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzBadgeModule } from 'ng-zorro-antd/badge';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NgOptimizedImage } from '@angular/common';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';



@Component({
  selector: 'app-notification-dropdown',
  imports: [NzDropDownModule,
    NzIconModule,
    NzBadgeModule,
    NzButtonModule,
    NzDividerModule,
    TimeAgoPipe,
    NgOptimizedImage,],
  templateUrl: './notification-dropdown.html',
  styleUrl: './notification-dropdown.scss'
})
export class NotificationDropdown {
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  // --- State Signals ---
  notifications = this.notificationService.allNotifications;
  unreadCount = this.notificationService.unreadNotificationsCount;

  /**
   * Handles the click event on a single notification.
   * Marks it as read and navigates to the associated link.
   */
  onNotificationClick(notification: Notification): void {
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.notificationId);
    }
    this.router.navigateByUrl(notification.link);
  }

  /**
   * Handles the click event for marking all notifications as read.
   */
  onMarkAllAsRead(): void {
    this.notificationService.markAllAsRead();
  }
}
