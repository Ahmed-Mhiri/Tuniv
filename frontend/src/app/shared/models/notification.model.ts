// Defines the different types of notifications the system can handle.
export enum NotificationType {
  NEW_ANSWER = 'NEW_ANSWER',
  ANSWER_MARKED_AS_SOLUTION = 'ANSWER_MARKED_AS_SOLUTION',
  NEW_COMMENT_ON_ANSWER = 'NEW_COMMENT_ON_ANSWER',
  NEW_REPLY_TO_COMMENT = 'NEW_REPLY_TO_COMMENT',
  NEW_VOTE_ON_QUESTION = 'NEW_VOTE_ON_QUESTION',
  NEW_VOTE_ON_ANSWER = 'NEW_VOTE_ON_ANSWER',
  NEW_VOTE_ON_COMMENT = 'NEW_VOTE_ON_COMMENT',
  NEW_QUESTION_IN_UNI = 'NEW_QUESTION_IN_UNI',
  NEW_CHAT_MESSAGE = 'NEW_CHAT_MESSAGE',
  WELCOME_TO_UNIVERSITY = 'WELCOME_TO_UNIVERSITY',
}

// Represents a single notification object received from the backend.
export interface Notification {
  notificationId: number;
  actorUsername: string;
  message: string;
  link: string;
  isRead: boolean;
  createdAt: string; // Transferred as an ISO string
  type: NotificationType;
}