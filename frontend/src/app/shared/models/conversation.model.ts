export interface Conversation {
  conversationId: number;
  participantName: string;
  participantAvatarUrl?: string;
  lastMessage: string;
  lastMessageTimestamp: string;
  unreadCount: number;
}