export interface ChatMessage {
  messageId: number;
  conversationId: number;
  senderUsername: string;
  content: string;
  sentAt: string; // ISO string format
  attachments: Attachment[];
  status?: 'sending' | 'sent' | 'error'; // Add this optional property
}

export interface Attachment {
  fileUrl: string;
  fileName: string;
  fileType: string;
}