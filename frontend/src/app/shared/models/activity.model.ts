/**
 * Represents a single item in a user's activity feed.
 * This interface matches the UserActivityItemDto from the backend.
 */
export interface UserActivityItem {
  type:
    | 'QUESTION_ASKED'
    | 'ANSWER_POSTED'
    | 'COMMENT_POSTED'
    | 'ACCEPTED_AN_ANSWER'
    | 'VOTE_CAST';
  createdAt: string; // The Instant from the backend is serialized as an ISO date string
  postScore: number;
  voteValue?: -1 | 1; // Optional, only present for 'VOTE_CAST' type
  questionId: number;
  questionTitle: string;
  answerId?: number;
  isSolution: boolean;
  commentId?: number;
}