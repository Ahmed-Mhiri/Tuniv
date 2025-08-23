import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';
import { AuthService } from '../services/auth.service';
import { DecodedToken } from '../../shared/models/auth.model'; // <-- IMPORT ADDED

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  if (!token) {
    return router.parseUrl('/login');
  }

  try {
    // Use the imported DecodedToken interface
    const decodedToken: DecodedToken = jwtDecode(token);
    const isAdmin = decodedToken.roles?.includes('admin');

    if (isAdmin) {
      return true; // User is an admin, allow access
    } else {
      // User is logged in but not an admin, redirect to home
      return router.parseUrl('/');
    }
  } catch (e) {
    // Token is invalid, redirect to login
    return router.parseUrl('/login');
  }
};