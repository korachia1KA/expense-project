import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../core/models';

/**
 * Turns any HTTP error into a sentence that can be shown to the user.
 *
 * Works hand in hand with the backend's GlobalExceptionHandler: every error body has the same
 * ApiError shape, so this ONE function can handle all of them:
 *  - status 0           → the request never reached a server (backend down, network issue)
 *  - fieldErrors present → "amount must be greater than 0; date cannot be in the future"
 *  - message present     → e.g. "Category 'Food' is used by 12 transactions..."
 *  - anything else       → the fallback text
 */
export function errorMessage(err: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Cannot reach the server. Is the backend running?';
    }
    const body = err.error as Partial<ApiError> | null;
    if (body?.fieldErrors && Object.keys(body.fieldErrors).length > 0) {
      return Object.entries(body.fieldErrors)
        .map(([field, msg]) => (msg.startsWith(field) || /^[A-Z]/.test(msg) ? msg : `${field} ${msg}`))
        .join('; ');
    }
    if (body?.message) {
      return body.message;
    }
  }
  return fallback;
}
