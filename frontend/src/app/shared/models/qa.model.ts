// src/app/shared/models/qa.model.ts

import { UserProfile } from './user.model';

// Re-using UserProfile as the Author model is a good practice
export type Author = Pick<UserProfile, 'userId' | 'username' | 'profilePhotoUrl'>;

export interface Comment {
  commentId: number;
  body: string;
  createdAt: string; // Dates are strings over HTTP
  author: Author;
  score: number;
  currentUserVote: number; // -1 for downvote, 0 for no vote, 1 for upvote
  children: Comment[]; // For nested replies
}

export interface Answer {
  answerId: number;
  body: string;
  isSolution: boolean;
  createdAt: string;
  author: Author;
  score: number;
  currentUserVote: number;
  comments: Comment[]; // Top-level comments for this answer
}

export interface Question {
  questionId: number;
  title: string;
  body: string;
  createdAt: string;
  author: Author;
  score: number;
  currentUserVote: number;
  answers: Answer[];
}

// --- Request Models ---

export interface CommentCreateRequest {
  body: string;
  parentCommentId?: number | null; // Optional for replying to another comment
}

export interface VoteRequest {
  value: 1 | -1;
}