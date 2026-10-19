import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * HTTP interceptor: a function that runs for EVERY request made with HttpClient.
 *
 * 1. Adds "Authorization: Bearer <jwt>" so the backend's JwtAuthenticationFilter knows who is
 *    calling. HttpRequest objects are immutable, so we clone() the request with the extra header.
 * 2. If the server answers 401 (token expired/invalid), logs the user out so they land on the
 *    login page instead of looking at a broken screen.
 *
 * Registered in app.config.ts via provideHttpClient(withInterceptors([authInterceptor])).
 * Being a plain function (not a class), it uses inject() to get the AuthService.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token;

  const authReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // A 401 from /api/auth/login just means "wrong password": don't log out for that.
      if (error.status === 401 && !req.url.includes('/api/auth/')) {
        auth.logout();
      }
      // Re-throw so the component that made the call can still show an error message.
      return throwError(() => error);
    }),
  );
};
