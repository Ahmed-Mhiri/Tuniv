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
import { UniversityListPageComponent } from './features/university/pages/university-list-page/university-list-page.component';
import { ModuleListPageComponent } from './features/university/pages/module-list-page/module-list-page.component';
import { QuestionDetailPageComponent } from './features/qa/pages/question-detail-page/question-detail-page.component';

// --- Route Guards ---
import { authGuard } from './core/guards/auth.guard';
import { publicOnlyGuard } from './core/guards/public-only.guard';

export const routes: Routes = [
  // --- Public Routes (No change here) ---
  { path: '', component: HomePageComponent },
  { path: 'verify-email', component: VerifyEmailPageComponent },

  // --- Public-Only Routes (No change here) ---
  { path: 'login', component: LoginPageComponent, canActivate: [publicOnlyGuard] },
  { path: 'register', component: RegisterPageComponent, canActivate: [publicOnlyGuard] },
  { path: 'forgot-password', component: ForgotPasswordPageComponent, canActivate: [publicOnlyGuard] },
  { path: 'reset-password', component: ResetPasswordPageComponent, canActivate: [publicOnlyGuard] },

  // --- CONTENT VIEWING & USER-SPECIFIC ROUTES ---

  // ✅ KEPT authGuard - Settings is a private page for the logged-in user.
  { path: 'settings', component: SettingsPageComponent, canActivate: [authGuard] },
  
  // ✅ REMOVED authGuard to make user profiles public.
  { path: 'users/:id', component: ProfilePageComponent },
  
  // ✅ REMOVED authGuard to make the list of universities public.
  { path: 'universities', component: UniversityListPageComponent },

  // ✅ REMOVED authGuard to make the list of modules public.
  { path: 'universities/:id/modules', component: ModuleListPageComponent },


  // --- Q&A Feature Routes ---
  {
    path: 'qa/ask',
    loadComponent: () =>
      import('./features/qa/pages/ask-question-page/ask-question-page.component').then(c => c.AskQuestionPageComponent),
    // ✅ KEPT authGuard - Asking a question is a protected action.
    canActivate: [authGuard],
  },
  {
    path: 'questions/:id',
    component: QuestionDetailPageComponent,
    // ✅ REMOVED authGuard to allow anyone to view a question.
  },
  {
    path: 'modules/:moduleId/questions',
    loadComponent: () =>
      import('./features/qa/pages/question-list-page/question-list-page.component').then(c => c.QuestionListPageComponent),
    // ✅ REMOVED authGuard to allow anyone to view the questions for a module.
  },
];