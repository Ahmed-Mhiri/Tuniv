import { ChangeDetectionStrategy, Component, effect, ElementRef, inject, input, SecurityContext, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { finalize, retry, Subscription, switchMap, takeWhile, timer } from 'rxjs';

import { AuthService } from '../../../../core/services/auth.service';
import { ChatService } from '../../services/chat.service';
import { ChatMessage } from '../../../../shared/models/chat.model';
import { Conversation } from '../../../../shared/models/conversation.model';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';

// NG-ZORRO Modules
import { NzUploadFile, NzUploadModule } from 'ng-zorro-antd/upload';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';

@Component({
  selector: 'app-conversation-page',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink, TimeAgoPipe, NzUploadModule,
    NzButtonModule, NzInputModule, NzIconModule, NzSpinModule, NzAvatarModule, NzToolTipModule
  ],
  templateUrl: './conversation-page.component.html',
  styleUrl: './conversation-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConversationPageComponent {
  // --- Inputs ---
  conversation = input.required<Conversation>();

  // --- Dependencies ---
  private readonly chatService = inject(ChatService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  readonly sanitizer = inject(DomSanitizer);

  // --- Element Refs ---
  @ViewChild('messageContainer') private messageContainer!: ElementRef<HTMLDivElement>;

  // --- State Signals ---
  readonly messages = signal<ChatMessage[]>([]);
  readonly isLoading = signal(true);
  readonly currentUser = this.authService.currentUser;
  readonly fileList = signal<NzUploadFile[]>([]);
  readonly filesToUpload = signal<File[]>([]);
  readonly isSending = signal(false);

  private wsSubscription?: Subscription;

  // --- Form Definition ---
  private atLeastOneFieldValidator = (control: AbstractControl): ValidationErrors | null => {
    const content = control.get('content')?.value;
    const hasFiles = this.filesToUpload().length > 0;
    return !content?.trim() && !hasFiles ? { required: true } : null;
  };

  messageForm = this.fb.group({
    content: [''],
  }, { validators: this.atLeastOneFieldValidator });

  constructor() {
    // --- Effect 1: Handles data fetching and WebSocket connection ---
    effect((onCleanup) => {
      const currentConversation = this.conversation();
      this.isLoading.set(true);
      this.messages.set([]); // Clear previous messages

      // Fetch message history
      this.chatService.getMessageHistory(currentConversation.conversationId).subscribe(history => {
        this.messages.set(history);
        this.isLoading.set(false);
        // The scrolling is now handled by the second effect
      });

      // Watch for new messages
      this.wsSubscription = this.chatService.watchConversation(currentConversation.conversationId)
        .subscribe(newMessage => {
          if (
            newMessage.senderUsername !== this.currentUser()?.username &&
            !this.messages().some(m => m.messageId === newMessage.messageId)
          ) {
            this.messages.update(current => [...current, newMessage]);
            // Scrolling is handled by the second effect
          }
        });

      onCleanup(() => {
        this.wsSubscription?.unsubscribe();
      });
    });

    // --- Effect 2: Handles scrolling the view to the bottom ---
    effect(() => {
      // Establish a dependency on the messages signal
      this.messages();

      // Ensure the messageContainer element exists before trying to scroll
      if (this.messageContainer) {
        // Use setTimeout to wait for the DOM to be updated with the new messages
        setTimeout(() => this.scrollToBottom(), 0);
      }
    });
  }

  sendMessage(): void {
    if (this.messageForm.invalid || this.isSending()) return;

    this.isSending.set(true);
    const content = this.messageForm.value.content?.trim() ?? '';
    const files = this.filesToUpload();
    const conversationId = this.conversation().conversationId;

    const tempMessageId = Date.now() * -1;
    const tempAttachments = files.map(file => ({
      fileName: file.name, fileType: file.type, fileUrl: URL.createObjectURL(file)
    }));

    const tempMessage: ChatMessage = {
      messageId: tempMessageId,
      conversationId: conversationId,
      senderUsername: this.currentUser()?.username || 'Me',
      content: this.sanitizer.sanitize(SecurityContext.HTML, content) || '',
      sentAt: new Date().toISOString(),
      attachments: tempAttachments,
      status: 'sending'
    };

    this.messages.update(current => [...current, tempMessage]);
    // The scrolling effect will automatically trigger when messages are updated
    this.resetForm();

    this.chatService.sendMessage(conversationId, tempMessage.content, files)
      .pipe(finalize(() => this.isSending.set(false)))
      .subscribe({
        next: (response: { messageId: number }) => {
          this.pollForFinalMessage(response.messageId, tempMessageId, tempAttachments);
        },
        error: (err) => {
          console.error('Failed to send message:', err);
          this.messages.update(current =>
            current.map(msg => (msg.messageId === tempMessageId ? { ...msg, status: 'error' } : msg))
          );
          tempAttachments.forEach(att => URL.revokeObjectURL(att.fileUrl));
        },
      });
  }

  private pollForFinalMessage(finalMessageId: number, tempMessageId: number, tempAttachments: any[]): void {
    timer(500, 1500)
      .pipe(
        switchMap(() => this.chatService.getSingleMessage(finalMessageId)),
        retry(3),
        // Stop when the message has a processed attachment URL, or after ~15 seconds
        takeWhile(msg => !msg.attachments?.[0]?.fileUrl.startsWith('http'), true)
      )
      .subscribe(finalMessage => {
        if (finalMessage && (!tempAttachments.length || finalMessage.attachments?.[0]?.fileUrl.startsWith('http'))) {
          this.messages.update(current =>
            current.map(msg => (msg.messageId === tempMessageId ? finalMessage : msg))
          );
          tempAttachments.forEach(att => URL.revokeObjectURL(att.fileUrl));
        }
      });
  }

  private scrollToBottom(): void {
    try {
      const container = this.messageContainer.nativeElement;
      container.scrollTop = container.scrollHeight;
    } catch (err) {
      console.error('Could not scroll to bottom:', err);
    }
  }

  private resetForm(): void {
    this.messageForm.reset();
    this.fileList.set([]);
    this.filesToUpload.set([]);
  }

  beforeUpload = (file: NzUploadFile): boolean => {
    this.filesToUpload.update(list => [...list, file as unknown as File]);
    this.fileList.update(list => [...list, file]);
    this.messageForm.updateValueAndValidity();
    return false;
  };

  removeFile = (file: NzUploadFile): void => {
    this.fileList.update(list => list.filter(f => f.uid !== file.uid));
    this.filesToUpload.update(list => (list as unknown as NzUploadFile[]).filter(f => f.uid !== file.uid) as unknown as File[]);
    this.messageForm.updateValueAndValidity();
  };

  handleEnterPress(event: Event): void {
    if (!(event instanceof KeyboardEvent)) return;

    if (!event.shiftKey) {
      event.preventDefault();
      if (!this.isSending()) {
        this.sendMessage();
      }
    }
  }
}