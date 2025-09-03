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

  /**
   * ✅ [RESTORED] Toggles the visibility of the chat widget.
   */
  toggleWidget(): void {
    this.isWidgetOpen.update(isOpen => !isOpen);
  }

  startConversationWithUser(participant: {
    participantId: number;
    participantName: string;
    participantAvatarUrl?: string | null;
  }): void {
    if (!this.isWidgetOpen()) {
      this.isWidgetOpen.set(true);
    }

    const existingConv = this.conversations().find(
      (c) => c.participantId === participant.participantId
    );

    if (existingConv) {
      this.openConversation(existingConv);
      return;
    }

    this.chatService.findOrCreateConversation(participant.participantId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (newConversation) => {
          this.conversations.update((convs) => [newConversation, ...convs]);
          this.openConversation(newConversation);
        },
        error: (err) => {
          console.error('Failed to start conversation:', err);
          this.isWidgetOpen.set(false);
        },
      });
  }

  openConversation(conversation: Conversation): void {
    this.activeConversation.set(conversation);
    if (!this.isWidgetOpen()) {
      this.isWidgetOpen.set(true);
    }

    if (conversation.unreadCount > 0) {
      this.chatService.markAsRead(conversation.conversationId)
        .pipe(
          tap(() => {
            this.conversations.update((currentConvos) =>
              currentConvos.map((c) =>
                c.conversationId === conversation.conversationId
                  ? { ...c, unreadCount: 0 }
                  : c
              )
            );
          }),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe({
          error: (err) =>
            console.error('Failed to mark conversation as read', err),
        });
    }
  }

  closeConversation(): void {
    this.activeConversation.set(null);
  }
}