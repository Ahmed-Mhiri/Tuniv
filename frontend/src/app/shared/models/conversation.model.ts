export interface Conversation {
  conversationId: number;
  participantName: string;
  participantId: number; // ✅ Add this property
  participantAvatarUrl?: string;
  lastMessage: string;
  lastMessageTimestamp: string;
  unreadCount: number;

}