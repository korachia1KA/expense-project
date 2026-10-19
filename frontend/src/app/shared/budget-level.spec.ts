import { budgetLevel } from './budget-level';

describe('budgetLevel', () => {
  it('is green up to 80 %, amber above 80 % and red above 100 %', () => {
    expect(budgetLevel(0)).toBe('ok');
    expect(budgetLevel(80)).toBe('ok');
    expect(budgetLevel(80.1)).toBe('warn');
    expect(budgetLevel(100)).toBe('warn');
    expect(budgetLevel(100.5)).toBe('over');
    expect(budgetLevel(null)).toBe('ok');   // no budget set
  });
});
