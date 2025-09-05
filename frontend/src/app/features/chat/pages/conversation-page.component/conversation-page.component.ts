// conversation-page.component.ts
import { ChangeDetectionStrategy, Component, effect, ElementRef, inject, input, SecurityContext, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
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
import { ChatWidgetService } from '../../services/chat-widget.service';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzModalModule, NzModalService } from 'ng-zorro-antd/modal';
import { NzPopoverModule } from 'ng-zorro-antd/popover';
import { ForwardMessageModalComponent } from '../forward-message-modal.component/forward-message-modal.component';

@Component({
  selector: 'app-conversation-page',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink, TimeAgoPipe, NzUploadModule,
    NzButtonModule, NzInputModule, NzIconModule, NzSpinModule, NzAvatarModule, NzToolTipModule,NzDropDownModule,
    NzModalModule,NzPopoverModule
  ],
  templateUrl: './conversation-page.component.html',
  styleUrl: './conversation-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConversationPageComponent {
  // --- Properties ---
  conversation = input.required<Conversation>();
  private readonly chatService = inject(ChatService);
  private readonly authService = inject(AuthService);
  private readonly modal = inject(NzModalService);
  private readonly fb = inject(FormBuilder);
  readonly sanitizer = inject(DomSanitizer);
  private readonly chatWidgetService = inject(ChatWidgetService);
  @ViewChild('messageContainer') private messageContainer!: ElementRef<HTMLDivElement>;
  readonly messages = signal<ChatMessage[]>([]);
  readonly isLoading = signal(true);
  readonly currentUser = this.authService.currentUser;
  readonly fileList = signal<NzUploadFile[]>([]);
  readonly filesToUpload = signal<File[]>([]);
  readonly isSending = signal(false);
  readonly availableReactions = ['👍', '❤️', '😂', '🎉', '😢', '😮'];
  messageForm: FormGroup;
  private wsSubscription?: Subscription;

  // ✅ Signal to hold the message that the user wants to react to
  activeMessageForReaction = signal<ChatMessage | null>(null);

  // Store pending attachments that haven't been confirmed by server yet
  private pendingAttachments = new Map<number, any[]>();
  
  private atLeastOneFieldValidator = (control: AbstractControl): ValidationErrors | null => {
    const content = control.get('content')?.value;
    const hasFiles = this.filesToUpload().length > 0;
    return !content?.trim() && !hasFiles ? { required: true } : null;
  };

  constructor() {
    this.messageForm = this.fb.group({
      content: [''],
    }, { validators: this.atLeastOneFieldValidator });

    effect((onCleanup) => {
      const currentConversation = this.conversation();
      this.isLoading.set(true);
      this.messages.set([]);
      this.pendingAttachments.clear();

      this.chatService.getMessageHistory(currentConversation.conversationId).subscribe((history) => {
        const sanitizedHistory = history.map(msg => this.sanitizeMessageAttachments(msg));
        this.messages.set(sanitizedHistory);
        this.isLoading.set(false);
      });

      this.wsSubscription = this.chatService
        .watchConversation(currentConversation.conversationId)
        .subscribe((incomingMessage) => {
          this.messages.update((currentMessages) => {
            const existingIndex = this.findMessageIndex(currentMessages, incomingMessage);
            
            if (existingIndex !== -1) {
              const updatedMessages = [...currentMessages];
              const existingMessage = updatedMessages[existingIndex];
              
              let updatedMessage: ChatMessage = { ...incomingMessage, attachments: [] };
              
              if (!incomingMessage.attachments || incomingMessage.attachments.length === 0) {
                const clientTempId = incomingMessage.clientTempId || incomingMessage.messageId;
                const pending = this.pendingAttachments.get(clientTempId);
                if (pending) {
                  updatedMessage.attachments = pending;
                } else if (existingMessage.attachments && existingMessage.attachments.length > 0) {
                  updatedMessage.attachments = existingMessage.attachments;
                }
              } else {
                const sanitizedMsg = this.sanitizeMessageAttachments(incomingMessage);
                updatedMessage = { ...sanitizedMsg };
                
                if (existingMessage.attachments) {
                  existingMessage.attachments.forEach((att: any) => {
                    if (att._originalUrl) {
                      URL.revokeObjectURL(att._originalUrl);
                    }
                  });
                }
                
                const clientTempId = incomingMessage.clientTempId || incomingMessage.messageId;
                this.pendingAttachments.delete(clientTempId);
              }
              
              updatedMessage.status = undefined;
              updatedMessages[existingIndex] = updatedMessage;
              return updatedMessages;

            } else {
              const sanitizedMessage = this.sanitizeMessageAttachments(incomingMessage);
              if (!currentMessages.some(m => m.messageId === sanitizedMessage.messageId)) {
                return [...currentMessages, sanitizedMessage];
              }
              return currentMessages;
            }
          });

          this.chatWidgetService.updateConversationSummary(incomingMessage);
        });

      onCleanup(() => {
        this.wsSubscription?.unsubscribe();
        this.pendingAttachments.clear();
      });
    });

    effect(() => {
      this.messages();
      setTimeout(() => this.scrollToBottom(), 50);
    });
  }

  // Helper to find message index
  private findMessageIndex(messages: ChatMessage[], incomingMessage: any): number {
    if (incomingMessage.clientTempId) {
      const index = messages.findIndex(m => 
        m.messageId === incomingMessage.clientTempId || 
        m.messageId === incomingMessage.messageId
      );
      if (index !== -1) return index;
    }
    return messages.findIndex(m => m.messageId === incomingMessage.messageId);
  }

  // Helper method to sanitize attachment URLs
  private sanitizeMessageAttachments(message: ChatMessage): ChatMessage {
    if (!message.attachments || message.attachments.length === 0) {
      return message;
    }
    const sanitizedAttachments = message.attachments.map(att => {
      if (typeof att.fileUrl === 'string') {
        return {
          ...att,
          fileUrl: this.sanitizer.bypassSecurityTrustUrl(att.fileUrl)
        };
      }
      return att;
    });
    return {
      ...message,
      attachments: sanitizedAttachments
    };
  }

  sendMessage(): void {
    if (this.messageForm.invalid || this.isSending()) return;
    this.isSending.set(true);

    const content = this.messageForm.value.content?.trim() ?? '';
    const files = this.filesToUpload();
    const tempMessageId = Date.now() * -1;
    
    const tempAttachments = files.map((file) => {
      const originalUrl = URL.createObjectURL(file);
      return {
        fileName: file.name,
        fileType: file.type,
        fileUrl: this.sanitizer.bypassSecurityTrustUrl(originalUrl),
        _originalUrl: originalUrl,
        _isTemp: true
      };
    });
    
    if (tempAttachments.length > 0) {
      this.pendingAttachments.set(tempMessageId, tempAttachments);
    }
    
    const tempMessage: ChatMessage = {
      messageId: tempMessageId,
      conversationId: this.conversation().conversationId,
      senderUsername: this.currentUser()?.username || 'Me',
      content: content,
      sentAt: new Date().toISOString(),
      attachments: tempAttachments,
      status: 'sending',
    };
    
    this.messages.update((current) => [...current, tempMessage]);
    this.resetForm();

    this.chatService.sendMessage(
      tempMessage.conversationId, 
      tempMessage.content, 
      files, 
      tempMessageId
    )
    .pipe(finalize(() => this.isSending.set(false)))
    .subscribe({
      next: (response) => {
        console.log('Message sent successfully:', response);
        if (response && response.attachments && response.attachments.length > 0) {
          const sanitizedAttachments = response.attachments.map((att: any) => ({
            ...att,
            fileUrl: typeof att.fileUrl === 'string' 
              ? this.sanitizer.bypassSecurityTrustUrl(att.fileUrl)
              : att.fileUrl
          }));
          
          this.messages.update((current) =>
            current.map((msg) => {
              if (msg.messageId === tempMessageId) {
                msg.attachments?.forEach((att: any) => {
                  if (att._originalUrl) {
                    URL.revokeObjectURL(att._originalUrl);
                  }
                });
                this.pendingAttachments.set(tempMessageId, sanitizedAttachments);
                return {
                  ...msg,
                  attachments: sanitizedAttachments,
                  messageId: response.messageId || msg.messageId,
                  status: undefined
                };
              }
              return msg;
            })
          );
        }
      },
      error: (err) => {
        console.error('Failed to send message:', err);
        this.messages.update((current) =>
          current.map((msg) => 
            msg.messageId === tempMessageId 
              ? { ...msg, status: 'error' } 
              : msg
          )
        );
        tempAttachments.forEach((att) => {
          if (att._originalUrl) URL.revokeObjectURL(att._originalUrl);
        });
        this.pendingAttachments.delete(tempMessageId);
      },
    });
  }
  
  beforeUpload = (file: NzUploadFile): boolean => {
    const fileToAdd = (file.originFileObj || file) as File;
    this.filesToUpload.update((list) => [...list, fileToAdd]);
    this.fileList.update((list) => [...list, file]);
    this.messageForm.updateValueAndValidity();
    return false;
  };

  removeFile = (file: NzUploadFile): void => {
    const updatedFileList = this.fileList().filter((f) => f.uid !== file.uid);
    this.fileList.set(updatedFileList);
    const updatedFilesToUpload = updatedFileList.map((f) => (f.originFileObj || f) as File);
    this.filesToUpload.set(updatedFilesToUpload);
    this.messageForm.updateValueAndValidity();
  };

  private resetForm(): void {
    this.messageForm.reset();
    this.fileList.set([]);
    this.filesToUpload.set([]);
  }

  private scrollToBottom(): void {
    if (this.messageContainer?.nativeElement) {
      this.messageContainer.nativeElement.scrollTop = this.messageContainer.nativeElement.scrollHeight;
    }
  }

  handleEnterPress(event: KeyboardEvent): void {
    if (!event.shiftKey) {
      event.preventDefault();
      if (!this.isSending()) {
        this.sendMessage();
      }
    }
  }

  ngOnDestroy(): void {
    this.messages().forEach(msg => {
      msg.attachments?.forEach((att: any) => {
        if (att._originalUrl) {
          URL.revokeObjectURL(att._originalUrl);
        }
      });
    });
    this.pendingAttachments.clear();
  }

  deleteMessage(messageId: number): void {
    this.chatService.deleteMessage(messageId).subscribe({
      next: () => {
        console.log(`Deletion request sent for message ${messageId}`);
      },
      error: (err) => {
        console.error('Failed to delete message:', err);
      },
    });
  }
  
  toggleReaction(message: ChatMessage, emoji: string): void {
    if (!message.messageId || message.messageId < 0) {
      console.warn('Cannot react to temporary message');
      return;
    }
    const currentUser = this.currentUser()?.username;
    if (!currentUser) return;

    this.messages.update(currentMessages => {
      const messageIndex = currentMessages.findIndex(m => m.messageId === message.messageId);
      if (messageIndex === -1) return currentMessages;

      const updatedMessages = [...currentMessages];
      const targetMessage = { ...updatedMessages[messageIndex] };
      targetMessage.reactions = targetMessage.reactions ? [...targetMessage.reactions] : [];
      const reactionIndex = targetMessage.reactions.findIndex(r => r.emoji === emoji);

      if (reactionIndex === -1) {
        targetMessage.reactions.push({
          emoji: emoji,
          count: 1,
          users: [currentUser],
          reactedByCurrentUser: true
        });
      } else {
        const existingReaction = { ...targetMessage.reactions[reactionIndex] };
        existingReaction.users = [...existingReaction.users];
        const userIndex = existingReaction.users.indexOf(currentUser);
        
        if (userIndex !== -1) {
          existingReaction.count--;
          existingReaction.users.splice(userIndex, 1);
          existingReaction.reactedByCurrentUser = false;
        } else {
          existingReaction.count++;
          existingReaction.users.push(currentUser);
          existingReaction.reactedByCurrentUser = true;
        }

        if (existingReaction.count === 0) {
          targetMessage.reactions.splice(reactionIndex, 1);
        } else {
          targetMessage.reactions[reactionIndex] = existingReaction;
        }
      }
      updatedMessages[messageIndex] = targetMessage;
      return updatedMessages;
    });

    this.chatService.toggleReaction(message.messageId, emoji).subscribe({
      next: (response) => console.log('Reaction toggle successful:', response),
      error: (err) => console.error('Failed to toggle reaction:', err)
    });
  }

  openForwardModal(messageToForward: ChatMessage): void {
    const allConversations = this.chatWidgetService.conversations();
    const currentConversationId = this.conversation().conversationId;
    const filteredConversations = allConversations.filter(
      conv => conv.conversationId !== currentConversationId
    );

    this.modal.create({
      nzTitle: 'Forward message to...',
      nzContent: ForwardMessageModalComponent,
      nzData: {
        message: messageToForward,
        conversations: filteredConversations 
      },
      nzOnOk: (componentInstance) => {
        const selectedIds = componentInstance.getSelectedIds();
        this.forwardMessage(messageToForward, selectedIds);
      }
    });
  }

  private forwardMessage(originalMessage: ChatMessage, selectedIds: number[]): void {
    if (selectedIds.length === 0) return;
    const formattedContent = `----------\nForwarded Message:\n${originalMessage.content}\n----------`;
    selectedIds.forEach(conversationId => {
      this.chatService.sendMessage(conversationId, formattedContent, [], Date.now() * -1)
        .subscribe({
          next: () => console.log(`Message forwarded to conversation ${conversationId}`),
          error: (err) => console.error(`Failed to forward to ${conversationId}`, err)
        });
    });
  }

  // ✅ New method to store the active message
  setActiveMessage(message: ChatMessage): void {
    this.activeMessageForReaction.set(message);
  }

  // ✅ Final, simplified method that uses the signal
  handleEmojiClick(emoji: string, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    
    // Get the message that was stored when the popover was opened
    const message = this.activeMessageForReaction();
  
    // Guard to ensure the message exists
    if (!message) {
      console.error('Could not find the active message to react to.');
      return;
    }
    
    console.log('Emoji clicked:', emoji, 'for message:', message.messageId);
    
    this.toggleReaction(message, emoji);
    
    // You might need to add logic here to close the popover manually
  }
}