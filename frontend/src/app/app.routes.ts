import { Routes } from '@angular/router';

// --- Page Components ---
import { HomePageComponent } from './features/home/pages/home-page/home-page.component';
import { LoginPageComponent } from './features/auth/pages/login-page/login-page.component';
import { RegisterPageComponent } from './features/auth/pages/register-page/register-page.component';
import { ForgotPasswordPageComponent } from './features/auth/pages/forgot-password-page/forgot-password-page';
import { ResetPasswordPageComponent } from './features/auth/pages/reset-password-page/reset-password-page';
import { VerifyEmailPageComponent } from './features/auth/pages/verify-email-page/verify-email-page';
import { SettingsPageComponent } from './features/user/pages/settings-page/settings-page.component';
import { ProfilePageComponent } from './features/user/pages/profile-page/profile-page.component';

// --- Route Guards ---
import { authGuard } from './core/guards/auth.guard';
import { publicOnlyGuard } from './core/guards/public-only.guard';

export const routes: Routes = [
  // --- Public Routes ---
  { path: '', component: HomePageComponent },
  { path: 'verify-email', component: VerifyEmailPageComponent },
  // FIX: Added placeholder route for universities. This should be accessible to everyone.
  { path: 'universities', component: HomePageComponent }, // TODO: Create and replace with UniversitiesPageComponent

  // --- Public-Only Routes (for logged-out users) ---
  { path: 'login', component: LoginPageComponent, canActivate: [publicOnlyGuard] },
  { path: 'register', component: RegisterPageComponent, canActivate: [publicOnlyGuard] },
  { path: 'forgot-password', component: ForgotPasswordPageComponent, canActivate: [publicOnlyGuard] },
  { path: 'reset-password', component: ResetPasswordPageComponent, canActivate: [publicOnlyGuard] },

  // --- Private Routes (for logged-in users) ---
  { path: 'settings', component: SettingsPageComponent, canActivate: [authGuard] },
  { path: 'users/:id', component: ProfilePageComponent, canActivate: [authGuard] },
  // FIX: Added placeholder route for asking questions. This requires a user to be logged in.
  { path: 'qa/ask', component: HomePageComponent, canActivate: [authGuard] }, // TODO: Create and replace with AskQuestionPageComponent
];