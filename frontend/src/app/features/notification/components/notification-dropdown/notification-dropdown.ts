import { Component, inject } from '@angular/core';
import { NotificationService } from '../../services/notification.service';
import { Router } from '@angular/router';
import { Notification, NotificationType } from '../../../../shared/models/notification.model';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzBadgeModule } from 'ng-zorro-antd/badge';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NgOptimizedImage } from '@angular/common';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';
import { ChatWidgetService } from '../../../chat/services/chat-widget.service';



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
  private readonly chatWidgetService = inject(ChatWidgetService); // ✅ 2. Inject the ChatWidgetService


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

    if (notification.type === NotificationType.NEW_CHAT_MESSAGE) {
      // It's a chat notification. We need to find the full conversation object.
      const conversationId = parseInt(notification.link.split('/').pop() || '', 10);

      if (isNaN(conversationId)) return;

      // ✅ 1. Get the full list of conversations from the service's signal.
      const allConversations = this.chatWidgetService.conversations();

      // ✅ 2. Find the specific conversation object using its ID.
      const conversationToOpen = allConversations.find(
        c => c.conversationId === conversationId
      );

      if (conversationToOpen) {
        // ✅ 3. If found, call the correct 'openConversation' method on the service
        //    with the full conversation object.
        this.chatWidgetService.openConversation(conversationToOpen);
      } else {
        // ✅ 4. Handle the case where the conversation isn't in the list yet.
        //    We refresh the list and open the widget so the user can see it.
        console.warn(`Conversation ${conversationId} not found in list. Refreshing...`);
        this.chatWidgetService.loadConversations();
        this.chatWidgetService.isWidgetOpen.set(true);
      }
    } else {
      // For all other types, it's a link to a page, so we use the router.
      this.router.navigateByUrl(notification.link);
    }
  }

  /**
   * Handles the click event for marking all notifications as read.
   */
  onMarkAllAsRead(): void {
    this.notificationService.markAllAsRead();
  }
   onDeleteOne(event: MouseEvent, notificationId: number): void {
    event.stopPropagation(); // Prevent the main onNotificationClick from firing
    this.notificationService.deleteNotification(notificationId);
  }

  onDeleteAll(): void {
    this.notificationService.deleteAllNotifications();
  }
}
