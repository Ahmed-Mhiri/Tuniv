// src/app/shared/models/qa.model.ts

// Represents the author of any post.
export interface Author {
  userId: number;
  username: string;
  profilePhotoUrl: string | null;
}

// Represents an uploaded file attachment.
export interface Attachment {
  fileName: string;
  fileUrl: string;
  fileType: string;
}

// Represents a single comment, which can have child comments.
export interface Comment {
  commentId: number;
  body: string;
  createdAt: string; // Dates are transferred as strings
  author: Author;
  score: number;
  currentUserVote: number; // -1, 0, or 1
  attachments: Attachment[];
  children: Comment[]; // For nested replies
}

// Represents a single answer to a question.
export interface Answer {
  answerId: number;
  body: string;
  isSolution: boolean;
  createdAt: string;
  author: Author;
  score: number;
  currentUserVote: number;
  attachments: Attachment[];
  comments: Comment[]; // Top-level comments for this answer
}

// Represents the entire question page data structure.
export interface Question {
  questionId: number;
  title: string;
  body: string;
  createdAt: string;
  author: Author;
  score: number;
  currentUserVote: number;
  attachments: Attachment[];
  answers: Answer[];
}

// --- Request Models ---

// For creating a new comment or a reply.
export interface CommentCreateRequest {
  body: string;
  parentCommentId?: number | null;
}

// For casting a vote.
export interface VoteRequest {
  value: 1 | -1;
}