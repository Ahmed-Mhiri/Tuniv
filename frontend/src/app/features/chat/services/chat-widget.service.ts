import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timer, tap } from 'rxjs';
import { ChatService } from './chat.service'; // ✅ Use your existing ChatService
import { Conversation } from '../../../shared/models/conversation.model';

@Injectable({
  providedIn: 'root'
})
export class ChatWidgetService {
  // --- Dependencies ---
  private readonly chatService = inject(ChatService);
  private readonly destroyRef = inject(DestroyRef);

  // --- State Signals ---
  readonly isWidgetOpen = signal(false);
  readonly activeConversation = signal<Conversation | null>(null);
  readonly conversations = signal<Conversation[]>([]);
  readonly isLoading = signal(true);

  constructor() {
    // Poll for new conversation data every 30 seconds to keep the list fresh.
    timer(0, 30000) // Fires immediately, then every 30s
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadConversations());
  }

  /**
   * Fetches the conversation list from the API and updates the state.
   */
  loadConversations(): void {
    if (!this.isLoading()) {
      this.isLoading.set(true);
    }
    
    this.chatService.getConversations().subscribe({
      next: (data) => {
        this.conversations.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load conversations', err);
        this.isLoading.set(false);
      },
    });
  }

  // --- Public API Methods ---
  toggleWidget(): void {
    this.isWidgetOpen.update(isOpen => !isOpen);
  }

  /**
   * Opens a specific conversation and marks it as read if necessary.
   */
  openConversation(conversation: Conversation): void {
    this.activeConversation.set(conversation);
    if (!this.isWidgetOpen()) {
      this.isWidgetOpen.set(true);
    }

    // If the conversation has unread messages, mark it as read.
    if (conversation.unreadCount > 0) {
      this.chatService.markAsRead(conversation.conversationId)
        .pipe(
          // This tap provides instant UI feedback without waiting for the next poll.
          tap(() => {
            this.conversations.update(currentConvos =>
              currentConvos.map(c =>
                c.conversationId === conversation.conversationId
                  ? { ...c, unreadCount: 0 }
                  : c
              )
            );
          }),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe({
          error: err => console.error('Failed to mark conversation as read', err),
        });
    }
  }

  /**
   * Closes the active conversation view and returns to the list.
   */
  closeConversation(): void {
    this.activeConversation.set(null);
  }
}