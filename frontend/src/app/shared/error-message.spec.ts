import { HttpErrorResponse } from '@angular/common/http';
import { errorMessage } from './error-message';

describe('errorMessage', () => {
  it('explains a network failure (status 0)', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('Cannot reach the server');
  });

  it('joins field errors from the backend ApiError body', () => {
    const err = new HttpErrorResponse({
      status: 400,
      error: { message: 'Validation failed', fieldErrors: { amount: 'must be greater than 0' } },
    });
    expect(errorMessage(err)).toBe('amount must be greater than 0');
  });

  it('uses the backend message for conflicts', () => {
    const err = new HttpErrorResponse({
      status: 409,
      error: { message: "Category 'Food' is used by 3 transactions.", fieldErrors: {} },
    });
    expect(errorMessage(err)).toBe("Category 'Food' is used by 3 transactions.");
  });
});
