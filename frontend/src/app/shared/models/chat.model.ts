export interface ChatAttachment {
  fileName: string;
  fileUrl: string;
  fileType: string;
}
export interface ChatMessage {
    messageId: number; // <-- ADD THIS
  content: string;
  senderUsername: string;
  sentAt: string; // ISO date string
  attachments: ChatAttachment[];

}