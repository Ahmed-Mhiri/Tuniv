import { Component, inject } from '@angular/core';
import { ChatWidgetService } from '../../../features/chat/services/chat-widget.service';
import { Conversation } from '../../models/conversation.model';
import { NzEmptyComponent, NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzBadgeComponent, NzBadgeModule } from 'ng-zorro-antd/badge';
import { CommonModule } from '@angular/common';
import { NzListModule } from 'ng-zorro-antd/list';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { TimeAgoPipe } from '../../pipes/time-ago.pipe';

@Component({
  selector: 'app-conversation-list',
  imports: [CommonModule,
    NzListModule,
    NzAvatarModule,
    NzBadgeModule,
    NzEmptyModule,
    TimeAgoPipe,],
  templateUrl: './conversation-list.component.html',
  styleUrl: './conversation-list.component.scss'
})
export class ConversationListComponent {
  private readonly chatWidgetService = inject(ChatWidgetService);

  // Expose the conversations signal directly to the template
  readonly conversations = this.chatWidgetService.conversations;

  /**
   * Tells the service to open the selected conversation view.
   * @param conversation The conversation object that was clicked.
   */
  openConversation(conversation: Conversation): void {
    this.chatWidgetService.openConversation(conversation);
  }
}
