import { Injectable, OnDestroy, inject } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { IMessage } from '@stomp/stompjs';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private client: RxStomp;
  private readonly authService = inject(AuthService);

  constructor() {
    this.client = new RxStomp();
  }

  connect(): void {
    if (!this.client.active) {
      this.client.configure({
        // =========================================================================
        // ✅ THE FIX: Revert to using brokerURL for a direct connection.
        // We no longer need the webSocketFactory or the SockJS library.
        // =========================================================================
        brokerURL: `ws://localhost:8080/ws`,

        connectHeaders: {
          Authorization: `Bearer ${this.authService.getToken()}`
        },
        
        reconnectDelay: 5000,
      });
      this.client.activate();
    }
  }

  // ... rest of your service is unchanged ...

  disconnect(): void {
    if (this.client.active) {
      this.client.deactivate();
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
  
  watch<T>(topic: string): Observable<T> {
    return this.client.watch(topic).pipe(
      map((message: IMessage) => JSON.parse(message.body) as T)
    );
  }

  sendMessage(destination: string, body: object): void {
    this.client.publish({
      destination: destination,
      body: JSON.stringify(body),
    });
  }
}