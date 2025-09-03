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
import { UniversityListPageComponent } from './features/university/pages/university-list-page/university-list-page.component'; // <-- Import
import { ModuleListPageComponent } from './features/university/pages/module-list-page/module-list-page.component'; // <-- Import

// --- Route Guards ---
import { authGuard } from './core/guards/auth.guard';
import { publicOnlyGuard } from './core/guards/public-only.guard';
import { QuestionDetailPageComponent } from './features/qa/pages/question-detail-page/question-detail-page.component';

export const routes: Routes = [
  // --- Public Routes ---
  { path: '', component: HomePageComponent },
  { path: 'verify-email', component: VerifyEmailPageComponent },

  // --- Public-Only Routes (for users who are not logged in) ---
  { path: 'login', component: LoginPageComponent, canActivate: [publicOnlyGuard] },
  { path: 'register', component: RegisterPageComponent, canActivate: [publicOnlyGuard] },
  { path: 'forgot-password', component: ForgotPasswordPageComponent, canActivate: [publicOnlyGuard] },
  { path: 'reset-password', component: ResetPasswordPageComponent, canActivate: [publicOnlyGuard] },

  // --- Private Routes (for logged-in users) ---
  { path: 'settings', component: SettingsPageComponent, canActivate: [authGuard] },
  { path: 'users/:id', component: ProfilePageComponent, canActivate: [authGuard] },
  { path: 'universities', component: UniversityListPageComponent, canActivate: [authGuard] },
  { path: 'universities/:id/modules', component: ModuleListPageComponent, canActivate: [authGuard] },

  // --- Q&A Feature Routes ---
  {
    path: 'qa/ask',
    loadComponent: () =>
      import('./features/qa/pages/ask-question-page/ask-question-page.component').then(c => c.AskQuestionPageComponent),
    canActivate: [authGuard],
  },
  {
    // This is the main route for viewing a question. It does NOT have a /qa prefix.
    path: 'questions/:id',
    component: QuestionDetailPageComponent,
    canActivate: [authGuard], // Assuming users must be logged in to see questions
  },
  {
    path: 'modules/:moduleId/questions',
    loadComponent: () =>
      import('./features/qa/pages/question-list-page/question-list-page.component').then(c => c.QuestionListPageComponent),
    canActivate: [authGuard],
  },
];



