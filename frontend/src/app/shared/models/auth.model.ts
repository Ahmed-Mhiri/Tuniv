// This interface is now used for both steps of the 2FA login
export interface AuthRequest {
  username: string;
  password: string;
  code?: string; // Optional: For the 2FA verification step
}

export interface AuthResponse {
  token: string | null;
  userId: number; // <-- Renamed from 'id' to match UserProfile
  username: string;
  email: string;
  profilePhotoUrl?: string | null;
  bio: string;             // <-- ADD
  major: string;           // <-- ADD
  reputationScore: number; // <-- ADD
  is2faRequired?: boolean;
  is2faEnabled?: boolean;
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