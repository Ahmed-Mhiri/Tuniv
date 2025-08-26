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