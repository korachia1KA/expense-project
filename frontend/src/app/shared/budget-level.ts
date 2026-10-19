/**
 * Traffic-light level for a budget's percentage used:
 *   ≤ 80 %  → 'ok'   (green)
 *   > 80 %  → 'warn' (amber: getting close)
 *   > 100 % → 'over' (red: limit exceeded)
 *
 * The CSS classes .level-warn / .level-over in styles.scss pick the colors. Keeping the
 * thresholds in one function means the dashboard and the budgets page can never disagree.
 */
export type BudgetLevel = 'ok' | 'warn' | 'over';

export const WARN_THRESHOLD = 80;
export const OVER_THRESHOLD = 100;

export function budgetLevel(percentUsed: number | null | undefined): BudgetLevel {
  if (percentUsed == null) return 'ok';
  if (percentUsed > OVER_THRESHOLD) return 'over';
  if (percentUsed > WARN_THRESHOLD) return 'warn';
  return 'ok';
}
