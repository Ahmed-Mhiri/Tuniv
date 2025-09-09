import { ModuleSummary } from "./university.model";

export interface Author {
  userId: number;
  username: string;
  profilePhotoUrl: string | null;
}

// Represents an uploaded file attachment.
export interface Attachment {
  attachmentId: number;
  fileName: string;
  fileUrl: string;
  fileType: string;
}

// Represents a single comment, which can have child comments.
export interface Comment {
  commentId: number;
  body: string;
  createdAt: string;
  author: Author;
  score: number;
  currentUserVote: number;
  attachments: Attachment[];
  children: Comment[];
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
  comments: Comment[];
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
  module: ModuleSummary; // ✅ ADD THIS PROPERTY
}

// --- Request Models ---

export interface QuestionCreateRequest {
  title: string;
  body: string;
  // ✅ FIX: Added moduleId to link the new question to a module.
  moduleId: number;
}

export interface AnswerCreateRequest {
  body: string;
}

export interface CommentCreateRequest {
  body: string;
  parentCommentId?: number | null;
  // ✅ FIX: Added answerId to link the new comment to an answer.
  answerId: number;
}

export interface VoteRequest {
  value: 1 | -1;
}

export interface QuestionUpdateRequest {
  title: string;
  body: string;
  attachmentIdsToDelete?: number[];
}

export interface AnswerUpdateRequest {
  body: string;
  attachmentIdsToDelete?: number[];
}

export interface CommentUpdateRequest {
  body: string;
  attachmentIdsToDelete?: number[];
}

// --- Response & DTO Models ---

export type QuestionResponseDto = Question;

export interface QuestionSummaryDto {
  id: number;
  title: string;
  authorId: number;
  authorUsername: string;
  createdAt: string;
  score: number;
  answerCount: number;
  currentUserVote: number;
}
