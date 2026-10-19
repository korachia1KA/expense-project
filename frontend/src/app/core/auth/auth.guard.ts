import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Route guards decide whether navigation to a route may happen. Returning a UrlTree
 * (router.createUrlTree) tells the router to redirect there instead.
 *
 * IMPORTANT: guards only improve the user experience. Real security is enforced by the backend,
 * which rejects every API call without a valid token whatever the browser does; anyone can edit
 * JavaScript in their own browser.
 */

/** Private pages: only logged-in users may enter; others are sent to /login. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? true : inject(Router).createUrlTree(['/login']);
};

/** Login/register pages: users who are already logged in go straight to the dashboard. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? inject(Router).createUrlTree(['/dashboard']) : true;
};
