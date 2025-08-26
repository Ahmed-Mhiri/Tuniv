import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * A guard to prevent authenticated users from accessing public-only routes like login and register.
 */
export const publicOnlyGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isUserLoggedIn()) {
    // If the user is already logged in, redirect them to the home page
    router.navigate(['/']);
    return false; // Block access to the requested route
  }

  // If the user is not logged in, allow access
  return true;
};