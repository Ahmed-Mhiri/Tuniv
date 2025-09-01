import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { WebSocketService } from '../../../core/services/websocket.service';
import { ChatMessage } from '../../../shared/models/chat.model';
import { Conversation } from '../../../shared/models/conversation.model'; // ✅ 1. Import Conversation model

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly webSocketService = inject(WebSocketService);
  private readonly apiUrl = environment.apiUrl;

  // --- Existing Methods ---

  getMessageHistory(conversationId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.apiUrl}/chat/${conversationId}/messages`);
  }

  watchConversation(conversationId: number): Observable<ChatMessage> {
    const topic = `/topic/conversation/${conversationId}`;
    return this.webSocketService.watch<ChatMessage>(topic);
  }

  sendMessage(conversationId: number, content: string, files: File[]): Observable<any> {
    if (files && files.length > 0) {
      const formData = new FormData();
      const messageDto = { content };
      formData.append('message', new Blob([JSON.stringify(messageDto)], { type: 'application/json' }));
      files.forEach(file => {
        formData.append('files', file, file.name);
      });
      return this.http.post(`${this.apiUrl}/chat/${conversationId}/message`, formData);
    } else {
      const destination = `/app/chat/${conversationId}/sendMessage`;
      const messageDto = { content };
      this.webSocketService.sendMessage(destination, messageDto);
      return new Observable(sub => sub.complete());
    }
  }

  getSingleMessage(messageId: number): Observable<ChatMessage> {
    return this.http.get<ChatMessage>(`${this.apiUrl}/messages/${messageId}`);
  }

  // ✅ 2. NEW METHODS ADDED FOR THE CHAT WIDGET
  
  /**
   * Fetches the conversation summary list for the current user.
   * Corresponds to: GET /api/v1/chat/conversations
   */
  getConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>(`${this.apiUrl}/chat/conversations`);
  }

  /**
   * Marks a conversation as read for the current user.
   * Corresponds to: POST /api/v1/chat/conversations/{conversationId}/read
   */
  markAsRead(conversationId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/chat/conversations/${conversationId}/read`, {});
  }
}