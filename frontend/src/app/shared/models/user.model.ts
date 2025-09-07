// This interface matches the UserProfileDto from your backend
export interface UserProfile {
  userId: number;
  username: string;
  profilePhotoUrl: string | null;
  bio: string;
  major: string;
  reputationScore: number;
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
  type: 'UNIVERSITY'; // Currently only supports University
  name: string;
  memberCount: number;
}

// --- NEW ---
// Matches the LeaderboardUserDto from the backend.
export interface LeaderboardUser {
  userId: number;
  username: string;
  reputationScore: number;
  profilePhotoUrl: string | null;
}