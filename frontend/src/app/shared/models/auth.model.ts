export interface AuthRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  id: number;
  username: string;
  email: string;
  profilePhotoUrl?: string | null;
}

export interface DecodedToken {
  exp: number;      // Expiration time
  sub: string;      // Subject (usually the username)
  id: number;
  email: string;
  roles?: string[]; // <-- ADDED: Roles are optional
}