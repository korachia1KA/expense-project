/**
 * Helpers for "YYYY-MM" month strings, the format the API uses for ?month=.
 *
 * These are plain functions (not a service) because they have no state and no dependencies:
 * easy to reuse and trivial to unit test (see month.spec.ts).
 *
 * Dates are built with `new Date(year, monthIndex, day)` which uses LOCAL time. Parsing a string
 * like new Date('2026-10-01') would use UTC and can show the previous day in some time zones.
 */

/** Pads a number to two digits: 3 → "03". */
const pad = (n: number) => String(n).padStart(2, '0');

/** The current month, e.g. "2026-10". */
export function currentMonth(today: Date = new Date()): string {
  return `${today.getFullYear()}-${pad(today.getMonth() + 1)}`;
}

/**
 * Moves a month forwards/backwards: shiftMonth('2026-01', -1) → "2025-12".
 * The Date constructor handles the year rollover (month index -1 = December of the previous year).
 */
export function shiftMonth(month: string, delta: number): string {
  const [y, m] = month.split('-').map(Number);
  const d = new Date(y, m - 1 + delta, 1);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
}

/** Human label: "2026-10" → "October 2026" (in the browser's language). */
export function monthLabel(month: string, style: 'long' | 'short' = 'long'): string {
  const [y, m] = month.split('-').map(Number);
  return new Intl.DateTimeFormat(undefined, { month: style, year: 'numeric' }).format(new Date(y, m - 1, 1));
}

/** Today as "YYYY-MM-DD" in local time (the format <input type="date"> uses). */
export function todayIso(today: Date = new Date()): string {
  return `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
}
