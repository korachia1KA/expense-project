import { ApplicationConfig, LOCALE_ID } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';

/**
 * Application-wide providers (this replaces the AppModule of older Angular versions).
 *
 * - provideRouter: enables routing. withComponentInputBinding() lets a route parameter such as
 *   ":id" arrive directly as a component @Input() (used by the transaction edit page).
 * - provideHttpClient: enables HttpClient and plugs in our JWT interceptor, which then runs for
 *   every request.
 * - LOCALE_ID: the locale used by pipes like CurrencyPipe and DatePipe. 'en-US' is built in;
 *   other locales need registerLocaleData(...).
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    { provide: LOCALE_ID, useValue: 'en-US' },
  ],
};
