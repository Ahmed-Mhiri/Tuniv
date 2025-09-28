// This interface matches the UserProfileDto from your backend
export interface UserProfile {
  userId: number;
  username: string;
  profilePhotoUrl: string | null;
  bio: string;
  major: string;
  reputationScore: number;
  email?: string; // Used in the mobile slide-out menu
  university?: string; // Used in the desktop profile widget
  questionsCount?: number; // Used for stats in profile widgets
  answersCount?: number; // Used for stats in profile widgets
  followersCount?: number; // Used for stats in profile widget
}

// This interface matches the UserProfileUpdateRequest from your backend
export interface UserProfileUpdateRequest {
  bio?: string;
  major?: string;
  profilePhotoUrl?: string;
}

// --- NEW ---
// Matches the CommunityDto from the backend for the "My Communities" feature.
export interface UserCommunity {
  id: number;
  type: 'UNIVERSITY';
  name: string;
  memberCount: number;
  questionsCount: number;
}

// --- NEW ---
// Matches the LeaderboardUserDto from the backend.
export interface LeaderboardUser {
  userId: number;
  username: string;
  reputationScore: number;
  profilePhotoUrl: string | null;
  answersCount: number; // Added for the mobile leaderboard card

}