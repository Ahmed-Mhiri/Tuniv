import { Injectable, signal, effect, inject, WritableSignal, Signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, Subscription } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { environment } from '../../../../environments/environment';
import { Notification } from '../../../shared/models/notification.model';



@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  // --- Injected Services ---
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly webSocketService = inject(WebSocketService);

  // --- Private State using Signals ---
  private readonly notifications: WritableSignal<Notification[]> = signal([]);
  private readonly unreadCount: WritableSignal<number> = signal(0);
  
  // --- Public Readonly Signals for Components ---
  public readonly allNotifications: Signal<Notification[]> = this.notifications.asReadonly();
  public readonly unreadNotificationsCount: Signal<number> = this.unreadCount.asReadonly();

  private readonly apiBaseUrl = environment.apiUrl;
  private notificationSub: Subscription | null = null;

  constructor() {
    // Use an effect to react to user login/logout from AuthService
    effect(() => {
      const user = this.authService.currentUser();
      if (user && user.username) {
        this.initialize(user.username);
      } else {
        this.cleanup();
      }
    });
  }

  /**
   * Initializes the service, fetches data, and connects to WebSockets.
   */
  private initialize(username: string): void {
    this.fetchInitialNotifications().subscribe();
    this.webSocketService.connect();

    const topic = `/topic/notifications/${username}`;
    this.notificationSub = this.webSocketService
      .watch<Notification>(topic)
      .subscribe((newNotification: Notification) => {
        // Add new notification and update unread count
        this.notifications.update(current => [newNotification, ...current]);
        this.unreadCount.update(count => count + 1);
      });
  }
  
  /**
   * Cleans up subscriptions and resets state upon logout.
   */
  private cleanup(): void {
    if (this.notificationSub) {
      this.notificationSub.unsubscribe();
      this.notificationSub = null;
    }
    this.webSocketService.disconnect();
    this.notifications.set([]);
    this.unreadCount.set(0);
  }

  private fetchInitialNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.apiBaseUrl}/notifications`).pipe(
      tap(notifications => {
        this.notifications.set(notifications);
        const unread = notifications.filter(n => !n.isRead).length;
        this.unreadCount.set(unread);
      }),
      catchError(() => {
        this.notifications.set([]);
        this.unreadCount.set(0);
        return of([]);
      })
    );
  }

  /**
   * Marks a single notification as read.
   */
  markAsRead(notificationId: number): void {
    // Optimistic update
    let wasUnread = false;
    this.notifications.update(notifications => {
      return notifications.map(n => {
        if (n.notificationId === notificationId && !n.isRead) {
          wasUnread = true;
          return { ...n, isRead: true };
        }
        return n;
      });
    });

    if (wasUnread) {
      this.unreadCount.update(count => count - 1);
    }
    
    // Persist change to the backend
    this.http.post(`${this.apiBaseUrl}/notifications/${notificationId}/read`, {}).pipe(
      catchError(err => {
        // Revert on error
        this.notifications.update(notifications => notifications.map(n => 
          n.notificationId === notificationId ? { ...n, isRead: false } : n
        ));
        if (wasUnread) {
          this.unreadCount.update(count => count + 1);
        }
        console.error('Failed to mark notification as read', err);
        return of(null);
      })
    ).subscribe();
  }

  /**
   * Marks all unread notifications as read.
   */
  markAllAsRead(): void {
    this.http.post(`${this.apiBaseUrl}/notifications/mark-all-read`, {}).pipe(
      tap(() => {
        // On success, update the local state
        this.notifications.update(notifications => 
          notifications.map(n => ({ ...n, isRead: true }))
        );
        this.unreadCount.set(0);
      }),
      catchError(err => {
        console.error('Failed to mark all notifications as read', err);
        return of(null);
      })
    ).subscribe();
  }
}