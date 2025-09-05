import { Component, inject, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzModalRef, NZ_MODAL_DATA } from 'ng-zorro-antd/modal';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { ChatMessage } from '../../../../shared/models/chat.model';
import { Conversation } from '../../../../shared/models/conversation.model';

@Component({
  selector: 'app-forward-message-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, NzCheckboxModule],
  templateUrl: './forward-message-modal.component.html',
  styleUrls: ['./forward-message-modal.component.scss']
})
export class ForwardMessageModalComponent implements OnInit {
  // --- Properties ---
  readonly message: ChatMessage;
  readonly conversations: Conversation[];
  private readonly modal = inject(NzModalRef);

  checkboxOptions: { label: string; value: number; checked: boolean }[] = [];

  /**
   * Injects modal data and assigns it to component properties.
   */
  constructor(@Inject(NZ_MODAL_DATA) data: { message: ChatMessage, conversations: Conversation[] }) {
    console.log('Data received in modal constructor:', data);
    this.message = data.message;
    this.conversations = data.conversations;
  }

  /**
   * Initializes the checkbox options when the component is ready.
   */
  ngOnInit(): void {
    this.checkboxOptions = this.conversations.map(c => ({
      label: c.participantName,
      value: c.conversationId,
      checked: false
    }));
  }

  /**
   * Called by the modal service to get the result when the user clicks 'Ok'.
   * @returns An array of selected conversation IDs.
   */
  getSelectedIds(): number[] {
    return this.checkboxOptions.filter(opt => opt.checked).map(opt => opt.value);
  }
}