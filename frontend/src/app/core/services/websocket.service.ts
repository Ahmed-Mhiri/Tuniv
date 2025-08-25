import { Injectable, OnDestroy, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private client: Client;
  isConnected = signal(false);

  constructor() {
    this.client = new Client({
      // The SockJS endpoint you configured in Spring Boot
      brokerURL: `ws://localhost:8080/ws`, // Use wss:// for production
      debug: (str) => { console.log(new Date(), str); },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // Update connection status signal on connect/disconnect
    this.client.onConnect = (frame) => {
      this.isConnected.set(true);
      console.log('Connected to WebSocket:', frame);
    };

    this.client.onDisconnect = (frame) => {
      this.isConnected.set(false);
      console.log('Disconnected from WebSocket:', frame);
    };

    this.client.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };
  }

  connect(): void {
    if (!this.client.active) {
      this.client.activate();
    }
  }

  disconnect(): void {
    if (this.client.active) {
      this.client.deactivate();
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  /**
   * Subscribe to a topic and return an Observable for the messages.
   * @param topic The topic to subscribe to (e.g., '/topic/conversation/123')
   */
  subscribe(topic: string): Observable<any> {
    return new Observable(observer => {
      const subscription: StompSubscription = this.client.subscribe(topic, (message: IMessage) => {
        // Parse the JSON message body and emit it
        observer.next(JSON.parse(message.body));
      });

      // On unsubscribe, deactivate the STOMP subscription
      return () => {
        if (subscription) {
          subscription.unsubscribe();
        }
      };
    });
  }

  /**
   * Send a message to a destination.
   * @param destination The destination to send to (e.g., '/app/chat.sendMessage/123')
   * @param body The message payload object.
   */
  sendMessage(destination: string, body: object): void {
    if (this.client.active) {
      this.client.publish({
        destination: destination,
        body: JSON.stringify(body),
      });
    } else {
      console.error('Cannot send message, WebSocket client is not active.');
    }
  }
}