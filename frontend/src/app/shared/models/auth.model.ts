// This interface is now used for both steps of the 2FA login
export interface AuthRequest {
  username: string;
  password: string;
  code?: string; // Optional: For the 2FA verification step
}

export interface AuthResponse {
  token: string | null;
  userId: number;
  username: string;
  email: string;
  profilePhotoUrl?: string | null;
  bio: string;
  major: string;
  reputationScore: number;
  is2faRequired?: boolean;
  is2faEnabled?: boolean;
  
  // ✅ ADD THESE PROPERTIES
  questionsCount: number;
  answersCount: number;
  followersCount: number; // It's good to add this one too for the stats card
}

export interface DecodedToken {
  exp: number;      // Expiration time
  sub: string;      // Subject (usually the username)
  id: number;
  email: string;
  roles?: string[];
}

// --- NEW INTERFACES ---

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface TwoFactorGenerateResponse {
  qrCodeUri: string; // The data URI for the QR code image
}