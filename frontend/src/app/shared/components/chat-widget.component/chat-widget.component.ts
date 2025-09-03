import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';



import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { ChatWidgetService } from '../../../features/chat/services/chat-widget.service';
import { ConversationPageComponent } from '../../../features/chat/pages/conversation-page.component/conversation-page.component';
import { ConversationListComponent } from "../conversation-list.component/conversation-list.component";

@Component({
  selector: 'app-chat-widget',
  standalone: true,
  imports: [
    NzIconModule,
    NzButtonModule,
    ConversationPageComponent,
    ConversationListComponent
],
  templateUrl: './chat-widget.component.html',
  styleUrls: ['./chat-widget.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatWidgetComponent {
  readonly chatWidgetService = inject(ChatWidgetService);

  readonly isWidgetOpen = this.chatWidgetService.isWidgetOpen;
  readonly activeConversation = this.chatWidgetService.activeConversation;

  // Derived title for the widget header
  readonly widgetTitle = computed(() => {
    return this.activeConversation()?.participantName ?? 'Messages';
  });

  goBackToList(): void {
    this.chatWidgetService.closeConversation();
  }
  
  
}