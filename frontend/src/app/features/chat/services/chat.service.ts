import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { WebSocketService } from '../../../core/services/websocket.service'; // Your existing service
import { ChatMessage } from '../../../shared/models/chat.model';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly webSocketService = inject(WebSocketService);
  private readonly apiUrl = environment.apiUrl;

  /**
   * Fetches the initial message history for a conversation via REST.
   */
  getMessageHistory(conversationId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/chat/${conversationId}/messages`);
  }

watchConversation(conversationId: number): Observable<ChatMessage> {
  const topic = `/topic/conversation/${conversationId}`;
  // ✅ FIX: Call the new .watch() method for a more reliable subscription
  return this.webSocketService.watch<ChatMessage>(topic);
}

  /**
   * Sends a message. It intelligently decides whether to use a REST endpoint
   * (for messages with files) or a WebSocket message (for text-only).
   */
  sendMessage(conversationId: number, content: string, files: File[]): Observable<any> {
    // If there are files, we must use the multipart REST endpoint.
    if (files && files.length > 0) {
      const formData = new FormData();
      const messageDto = { content };
      formData.append('message', new Blob([JSON.stringify(messageDto)], { type: 'application/json' }));
      files.forEach(file => {
        formData.append('files', file, file.name);
      });
      return this.http.post(`${this.apiUrl}/chat/${conversationId}/message`, formData);
    } 
    // For text-only messages, use the more efficient WebSocket endpoint.
    else {
      const destination = `/app/chat/${conversationId}/sendMessage`;
      const messageDto = { content };
      this.webSocketService.sendMessage(destination, messageDto);
      // Since WebSocket is fire-and-forget, we return an empty observable.
      return new Observable(sub => sub.complete());
    }
  }
}