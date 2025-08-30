import { ChangeDetectionStrategy, Component, OnInit, OnDestroy, inject, signal, ViewChild, ElementRef, SecurityContext, afterNextRender } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';
import { Subscription, switchMap } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { ChatService } from '../../services/chat.service';
import { ChatMessage } from '../../../../shared/models/chat.model';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';

// NG-ZORRO Modules
import { NzUploadChangeParam, NzUploadFile, NzUploadModule } from 'ng-zorro-antd/upload';
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
export class ConversationPageComponent implements OnInit, OnDestroy {
  @ViewChild('messageContainer') private messageContainer!: ElementRef;

  private readonly route = inject(ActivatedRoute);
  private readonly chatService = inject(ChatService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly sanitizer = inject(DomSanitizer);

  private wsSubscription: Subscription | null = null;

  readonly messages = signal<ChatMessage[]>([]);
  readonly isLoading = signal(true);
  readonly currentUser = this.authService.currentUser;

  readonly fileList = signal<NzUploadFile[]>([]);
  readonly filesToUpload = signal<File[]>([]);
  readonly isSending = signal(false);

  private atLeastOneFieldValidator = (control: AbstractControl): ValidationErrors | null => {
    const content = control.get('content')?.value;
    const hasFiles = this.filesToUpload().length > 0;
    return !content?.trim() && !hasFiles ? { required: true } : null;
  };

  messageForm = this.fb.group({
    content: [''],
  }, { validators: this.atLeastOneFieldValidator });

  constructor() {
    // This function will run AFTER Angular has rendered any changes to the DOM.
    // It's the perfect place to run DOM-manipulating code like scrolling.
    afterNextRender(() => {
      // This check is important because afterNextRender runs on initial load too.
      if (this.messages().length > 0) {
        this.scrollToBottom();
      }
    });
  }

  ngOnInit(): void {
    this.route.paramMap.pipe(
      switchMap(params => {
        const conversationId = Number(params.get('id'));
        this.isLoading.set(true);

        // When a new message arrives, just update the signal.
        // The afterNextRender function will handle the scrolling automatically.
        this.wsSubscription = this.chatService.watchConversation(conversationId).subscribe(newMessage => {
          this.messages.update(current => [...current, newMessage]);
        });

        return this.chatService.getMessageHistory(conversationId);
      })
    ).subscribe(history => {
      this.messages.set(history);
      this.isLoading.set(false);
    });
  }

  ngOnDestroy(): void {
    this.wsSubscription?.unsubscribe();
  }

  sendMessage(): void {
    if (this.messageForm.invalid) return;

    const content = this.messageForm.value.content?.trim() ?? '';
    const files = this.filesToUpload();

    this.isSending.set(true);
    const conversationId = Number(this.route.snapshot.paramMap.get('id'));

    const sanitizedContent = this.sanitizer.sanitize(SecurityContext.HTML, content) || '';

    this.chatService.sendMessage(conversationId, sanitizedContent, files).subscribe({
      complete: () => {
        this.messageForm.reset();
        this.fileList.set([]);
        this.filesToUpload.set([]);
        this.isSending.set(false);
        // We don't need to call scrollToBottom here anymore.
        // The WebSocket broadcast will update the signal, which triggers afterNextRender.
      },
      error: () => this.isSending.set(false),
    });
  }

  trackByMessage(index: number, message: ChatMessage): number {
    return message.messageId;
  }

  beforeUpload = (file: NzUploadFile): boolean => {
    this.filesToUpload.update(list => [...list, file as unknown as File]);
    this.fileList.update(list => [...list, file]);
    this.messageForm.updateValueAndValidity();
    return false; // Prevent automatic upload
  };

  removeFile = (file: NzUploadFile): void => {
    this.fileList.update(list => list.filter(f => f.uid !== file.uid));
    this.filesToUpload.update(list => (list as unknown as NzUploadFile[]).filter(f => f.uid !== file.uid) as unknown as File[]);
    this.messageForm.updateValueAndValidity();
  };

  private scrollToBottom(): void {
    try {
      this.messageContainer.nativeElement.scrollTop = this.messageContainer.nativeElement.scrollHeight;
    } catch (err) {
      console.error("Could not scroll to bottom:", err);
    }
  }

  handleEnterPress(event: Event): void {
  // First, check if the event is actually a KeyboardEvent
  if (!(event instanceof KeyboardEvent)) {
    return;
  }

  // Now TypeScript knows it's safe to access keyboard-specific properties
  if (!event.shiftKey) {
    event.preventDefault(); // Prevent new line on Enter
    this.sendMessage();
  }
}
}