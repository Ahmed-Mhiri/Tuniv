import { Routes } from '@angular/router';

// --- Page Components ---
import { HomePageComponent } from './features/home/pages/home-page/home-page.component';
import { LoginPageComponent } from './features/auth/pages/login-page/login-page.component';
import { RegisterPageComponent } from './features/auth/pages/register-page/register-page.component';
import { ForgotPasswordPageComponent } from './features/auth/pages/forgot-password-page/forgot-password-page';
import { ResetPasswordPageComponent } from './features/auth/pages/reset-password-page/reset-password-page';
import { SettingsPageComponent } from './features/user/pages/settings-page/settings-page.component'; // Assuming path

// --- Route Guards ---
import { authGuard } from './core/guards/auth.guard';
import { publicOnlyGuard } from './core/guards/public-only.guard';
import { VerifyEmailPageComponent } from './features/auth/pages/verify-email-page/verify-email-page';

export const routes: Routes = [
  // --- Public Routes ---
  { path: '', component: HomePageComponent },
  { path: 'verify-email', component: VerifyEmailPageComponent },

  // --- Public-Only Routes (for logged-out users) ---
  {
    path: 'login',
    component: LoginPageComponent,
    canActivate: [publicOnlyGuard], // <-- Protects this route from logged-in users
  },
  {
    path: 'register',
    component: RegisterPageComponent,
    canActivate: [publicOnlyGuard], // <-- Protects this route from logged-in users
  },
  {
    path: 'forgot-password',
    component: ForgotPasswordPageComponent,
    canActivate: [publicOnlyGuard], // <-- Protects this route from logged-in users
  },
  {
    path: 'reset-password',
    component: ResetPasswordPageComponent,
    canActivate: [publicOnlyGuard], // <-- Protects this route from logged-in users
  },

  // --- Private Routes (for logged-in users) ---
  {
    path: 'settings',
    component: SettingsPageComponent,
    canActivate: [authGuard], // <-- Protects this route from logged-out users
  },
  
  // Example of another private route
  // {
  //   path: 'ask-question',
  //   component: AskQuestionPageComponent,
  //   canActivate: [authGuard],
  // },

  // --- Wildcard Route ---
  // { path: '**', component: NotFoundPageComponent }, // Recommended to add a 404 page
];