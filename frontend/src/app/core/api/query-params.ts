import { HttpParams } from '@angular/common/http';

/**
 * Turns an object such as { month: '2026-10', type: '', page: 0 } into HttpParams
 * ("?month=2026-10&page=0"), skipping undefined/null/empty values so the backend treats
 * them as "no filter". HttpParams also URL-encodes values safely (spaces, &, etc.).
 */
export function toParams(query: object): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      params = params.set(key, String(value));
    }
  }
  return params;
}
