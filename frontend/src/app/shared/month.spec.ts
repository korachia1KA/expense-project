import { currentMonth, shiftMonth, todayIso } from './month';

// Plain functions need no TestBed: just call them and check the result.
describe('month helpers', () => {
  it('formats the current month and today with zero padding', () => {
    const date = new Date(2026, 2, 5); // 5 March 2026 (month index 2 = March)
    expect(currentMonth(date)).toBe('2026-03');
    expect(todayIso(date)).toBe('2026-03-05');
  });

  it('shifts months across year boundaries', () => {
    expect(shiftMonth('2026-01', -1)).toBe('2025-12');
    expect(shiftMonth('2025-12', 1)).toBe('2026-01');
    expect(shiftMonth('2026-10', -6)).toBe('2026-04');
  });
});
