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
  private readonly modal = inject(NzModalService); // ✅ INJECT
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
              
              let updatedMessage: ChatMessage = { ...incomingMessage, attachments: [] }; // Start with empty attachments
              
              if (!incomingMessage.attachments || incomingMessage.attachments.length === 0) {
                const clientTempId = incomingMessage.clientTempId || incomingMessage.messageId;
                
                // ✅ FIX: Safely get from the map and check for existence
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

  // Helper to find message index (checks both messageId and clientTempId)
  private findMessageIndex(messages: ChatMessage[], incomingMessage: any): number {
    // First try to match by clientTempId (for messages we sent)
    if (incomingMessage.clientTempId) {
      const index = messages.findIndex(m => 
        m.messageId === incomingMessage.clientTempId || 
        m.messageId === incomingMessage.messageId
      );
      if (index !== -1) return index;
    }
    
    // Then try to match by messageId
    return messages.findIndex(m => m.messageId === incomingMessage.messageId);
  }

  // Helper method to sanitize attachment URLs in a message
  private sanitizeMessageAttachments(message: ChatMessage): ChatMessage {
    if (!message.attachments || message.attachments.length === 0) {
      return message;
    }

    const sanitizedAttachments = message.attachments.map(att => {
      // Check if fileUrl needs sanitizing
      if (typeof att.fileUrl === 'string') {
        return {
          ...att,
          fileUrl: this.sanitizer.bypassSecurityTrustUrl(att.fileUrl)
        };
      }
      // Already sanitized or is a SafeUrl
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
    
    // Create temporary attachments with blob URLs for immediate preview
    const tempAttachments = files.map((file) => {
      const originalUrl = URL.createObjectURL(file);
      return {
        fileName: file.name,
        fileType: file.type,
        fileUrl: this.sanitizer.bypassSecurityTrustUrl(originalUrl),
        _originalUrl: originalUrl, // Store for cleanup
        _isTemp: true // Flag to identify temp attachments
      };
    });
    
    // Store attachments in pending map
    if (tempAttachments.length > 0) {
      this.pendingAttachments.set(tempMessageId, tempAttachments);
    }
    
    // Create temp message with immediate preview
    const tempMessage: ChatMessage = {
      messageId: tempMessageId,
      conversationId: this.conversation().conversationId,
      senderUsername: this.currentUser()?.username || 'Me',
      content: content,
      sentAt: new Date().toISOString(),
      attachments: tempAttachments,
      status: 'sending',
    };
    
    // Add to messages immediately for instant feedback
    this.messages.update((current) => [...current, tempMessage]);
    this.resetForm();

    // Send to server
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
        
        // If the response includes complete attachment data, update immediately
        if (response && response.attachments && response.attachments.length > 0) {
          const sanitizedAttachments = response.attachments.map((att: any) => ({
            ...att,
            fileUrl: typeof att.fileUrl === 'string' 
              ? this.sanitizer.bypassSecurityTrustUrl(att.fileUrl)
              : att.fileUrl
          }));
          
          // Update the message with server-confirmed attachments
          this.messages.update((current) =>
            current.map((msg) => {
              if (msg.messageId === tempMessageId) {
                // Clean up temp blob URLs
                msg.attachments?.forEach((att: any) => {
                  if (att._originalUrl) {
                    URL.revokeObjectURL(att._originalUrl);
                  }
                });
                
                // Update pending attachments with server URLs
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
        } else {
          // Server didn't return attachments, keep using temp ones
          console.log('Server response lacks attachments, keeping temporary preview');
        }
      },
      error: (err) => {
        console.error('Failed to send message:', err);
        
        // Mark message as error
        this.messages.update((current) =>
          current.map((msg) => 
            msg.messageId === tempMessageId 
              ? { ...msg, status: 'error' } 
              : msg
          )
        );
        
        // Clean up on error
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
    // Clean up any remaining blob URLs
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
        // The WebSocket update will handle the UI change, 
        // but you could add an optimistic update here for instant feedback.
        console.log(`Deletion request sent for message ${messageId}`);
      },
      error: (err) => {
        console.error('Failed to delete message:', err);
        // Optionally show a notification to the user
      },
    });
  }
  toggleReaction(message: ChatMessage, emoji: string): void {
    if (!message.messageId || message.messageId < 0) return;

    // We don't need to do anything in the 'next' block,
    // as the WebSocket broadcast will update the state for all users at once.
    this.chatService.toggleReaction(message.messageId, emoji).subscribe({
      error: (err) => console.error('Failed to toggle reaction:', err),
    });
  
  
  }

  openForwardModal(messageToForward: ChatMessage): void {
    let selectedConversationIds: number[] = [];
    const conversations = this.chatWidgetService.conversations();
    const checkboxOptions = conversations.map(c => ({
      label: c.participantName,
      value: c.conversationId,
      checked: false
    }));

    this.modal.create({
      nzTitle: 'Forward message to...',
      nzContent: `
        <p class="forward-preview">
          <strong>Message:</strong><br>
          <em>${messageToForward.content}</em>
        </p>
        <nz-checkbox-group [(ngModel)]="checkboxOptions"></nz-checkbox-group>
      `,
      nzOnOk: () => this.forwardMessage(messageToForward, checkboxOptions)
    });
  }

  private forwardMessage(
    originalMessage: ChatMessage,
    options: { label: string; value: number; checked: boolean }[]
  ): void {
    const selectedIds = options.filter(opt => opt.checked).map(opt => opt.value);
    
    if (selectedIds.length === 0) {
      return; // Or show a message
    }

    const formattedContent = `----------\nForwarded Message:\n${originalMessage.content}\n----------`;

    selectedIds.forEach(conversationId => {
      // NOTE: We pass an empty array for files, as we are only forwarding text.
      this.chatService.sendMessage(conversationId, formattedContent, [], Date.now() * -1)
        .subscribe({
          next: () => console.log(`Message forwarded to conversation ${conversationId}`),
          error: (err) => console.error(`Failed to forward to ${conversationId}`, err)
        });
    });
  }
}