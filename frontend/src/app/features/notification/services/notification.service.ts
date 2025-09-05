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

  // --- Private Writable State ---
  private readonly _notifications: WritableSignal<Notification[]> = signal([]);
  private readonly _unreadCount: WritableSignal<number> = signal(0);
  
  // --- Public Readonly Signals for Components ---
  public readonly allNotifications: Signal<Notification[]> = this._notifications.asReadonly();
  public readonly unreadNotificationsCount: Signal<number> = this._unreadCount.asReadonly();

  private readonly apiUrl = `${environment.apiUrl}/notifications`; // ✅ REFINEMENT: Centralized API URL
  private notificationSub: Subscription | null = null;

  constructor() {
    // React to user login/logout to initialize or clean up the service.
    effect(() => {
      const user = this.authService.currentUser();
      if (user && user.userId) { // Use a stable identifier like userId
        this.initialize(user.userId);
      } else {
        this.cleanup();
      }
    });
  }

  /**
   * Initializes the service, fetches initial data, and connects to the WebSocket.
   */
  private initialize(userId: number): void {
    this.fetchInitialNotifications().subscribe();
    this.webSocketService.connect();

    // User-specific topic for real-time notifications
    const topic = `/topic/user/${userId}/notifications`;
    this.notificationSub = this.webSocketService
      .watch<Notification>(topic)
      .subscribe((newNotification: Notification) => {
        this._notifications.update(current => [newNotification, ...current]);
        this._unreadCount.update(count => count + 1);
      });
  }
  
  private fetchInitialNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.apiUrl).pipe(
      tap(notifications => {
        this._notifications.set(notifications);
        this._unreadCount.set(notifications.filter(n => !n.isRead).length);
      }),
      catchError(() => {
        this._notifications.set([]);
        this._unreadCount.set(0);
        return of([]);
      })
    );
  }

  markAsRead(notificationId: number): void {
    // Find the notification first to see if it's actually unread
    const target = this.allNotifications().find(n => n.notificationId === notificationId);
    if (!target || target.isRead) {
      return; // Do nothing if already read or not found
    }

    // Optimistic update of the UI
    this._notifications.update(list =>
      list.map(n => n.notificationId === notificationId ? { ...n, isRead: true } : n)
    );
    this._unreadCount.update(count => count - 1);
    
    // Persist change to the backend
    this.http.post(`${this.apiUrl}/${notificationId}/read`, {}).pipe(
      catchError(err => {
        console.error('Failed to mark notification as read', err);
        // Revert UI changes on error
        this._notifications.update(list =>
          list.map(n => n.notificationId === notificationId ? { ...n, isRead: false } : n)
        );
        this._unreadCount.update(count => count + 1);
        return of(null);
      })
    ).subscribe();
  }

  markAllAsRead(): void {
    // ✅ FIX: Use the correct API endpoint from our backend implementation
    this.http.post(`${this.apiUrl}/mark-all-read`, {}).pipe(
      tap(() => {
        this._notifications.update(list => 
          list.map(n => n.isRead ? n : { ...n, isRead: true })
        );
        this._unreadCount.set(0);
      }),
      catchError(err => {
        console.error('Failed to mark all as read', err);
        return of(null);
      })
    ).subscribe();
  }
  
  deleteNotification(notificationId: number): void {
    const wasUnread = !!this.allNotifications().find(n => n.notificationId === notificationId && !n.isRead);

    // Optimistic update
    this._notifications.update(list =>
        list.filter(n => n.notificationId !== notificationId)
    );
    if (wasUnread) {
        this._unreadCount.update(c => c - 1);
    }
      
    this.http.delete(`${this.apiUrl}/${notificationId}`).pipe(
      catchError(err => {
        console.error('Failed to delete notification', err);
        // Revert on error by re-fetching the list
        this.fetchInitialNotifications().subscribe();
        return of(null);
      })
    ).subscribe();
  }

  deleteAllNotifications(): void {
    // Optimistic update
    this._notifications.set([]);
    this._unreadCount.set(0);

    // ✅ FIX: Use the correct API endpoint from our backend implementation
    this.http.delete(`${this.apiUrl}/all`).pipe(
      catchError(err => {
        console.error('Failed to delete all notifications', err);
        // Revert on error by re-fetching
        this.fetchInitialNotifications().subscribe();
        return of(null);
      })
    ).subscribe();
  }

  /**
   * Cleans up subscriptions and resets state upon logout.
   */
  private cleanup(): void {
    this.notificationSub?.unsubscribe();
    this.notificationSub = null;
    this.webSocketService.disconnect();
    this._notifications.set([]);
    this._unreadCount.set(0);
  }
}