import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Use the computed signal for a reactive check
  if (authService.isUserLoggedIn()) {
    return true; // User is logged in, allow access to the route
  }

  // User is not logged in, redirect them to the login page
  return router.parseUrl('/login');
};