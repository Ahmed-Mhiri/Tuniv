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
  // --- Properties ---
  conversation = input.required<Conversation>();
  private readonly chatService = inject(ChatService);
  private readonly authService = inject(AuthService);
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
  messageForm: FormGroup;
  private wsSubscription?: Subscription;
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

      this.chatService.getMessageHistory(currentConversation.conversationId).subscribe((history) => {
        this.messages.set(history);
        this.isLoading.set(false);
      });

      this.wsSubscription = this.chatService
        .watchConversation(currentConversation.conversationId)
        .subscribe((finalMessage) => {
          this.messages.update((currentMessages) => {
            const messageIndex = currentMessages.findIndex(
              (m) => m.messageId === finalMessage.clientTempId
            );

            if (messageIndex !== -1) {
              const updatedMessages = [...currentMessages];
              const oldMessage = updatedMessages[messageIndex];
              oldMessage.attachments?.forEach((att: any) => {
                if (att._originalUrl) URL.revokeObjectURL(att._originalUrl);
              });
              updatedMessages[messageIndex] = finalMessage;
              return updatedMessages;
            } else {
              if (!currentMessages.some(m => m.messageId === finalMessage.messageId)) {
                return [...currentMessages, finalMessage];
              }
              return currentMessages;
            }
          });

          this.chatWidgetService.updateConversationSummary(finalMessage);
        });

      onCleanup(() => {
        this.wsSubscription?.unsubscribe();
      });
    });

    effect(() => {
      this.messages();
      setTimeout(() => this.scrollToBottom(), 50);
    });
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
        fileName: file.name, fileType: file.type,
        fileUrl: this.sanitizer.bypassSecurityTrustUrl(originalUrl),
        _originalUrl: originalUrl,
      };
    });
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

    this.chatService.sendMessage(tempMessage.conversationId, tempMessage.content, files, tempMessageId)
      .pipe(finalize(() => this.isSending.set(false)))
      .subscribe({
        next: () => {
          console.log('File upload confirmed by server. Waiting for WebSocket message for UI update.');
        },
        error: (err) => {
          console.error('Failed to send message:', err);
          this.messages.update((current) =>
            current.map((msg) => msg.messageId === tempMessageId ? { ...msg, status: 'error' } : msg)
          );
          tempAttachments.forEach((att) => URL.revokeObjectURL(att._originalUrl));
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
}