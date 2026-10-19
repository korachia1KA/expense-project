import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { Summary } from '../../core/models';
import { currentMonth } from '../../shared/month';

/**
 * COMPONENT test: renders the real dashboard template with fake HTTP responses and checks
 * what the user would see. This catches template mistakes that a plain build cannot.
 */
describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let http: HttpTestingController;

  const summary: Summary = {
    month: currentMonth(),
    totalIncome: 3000,
    totalExpense: 1250.5,
    balance: 1749.5,
    savingsRate: 58.3,
    spendingByCategory: [
      { categoryId: 1, name: 'Food', color: '#f97316', spent: 450, budget: 400, percentUsed: 112.5 },
      { categoryId: 2, name: 'Transport', color: '#0ea5e9', spent: 85, budget: 100, percentUsed: 85 },
      { categoryId: 3, name: 'Housing', color: '#8b5cf6', spent: 715.5, budget: null, percentUsed: null },
    ],
    dailySpending: [{ date: `${currentMonth()}-01`, amount: 20 }],
    trend: [{ month: currentMonth(), income: 3000, expense: 1250.5 }],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DashboardComponent], // standalone components are imported, not declared
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(DashboardComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges(); // runs ngOnInit → sends both requests
  });

  it('shows the summary cards and colors budgets by level', () => {
    http.expectOne((r) => r.url === '/api/summary').flush(summary);
    http.expectOne((r) => r.url === '/api/transactions').flush({ content: [], page: 0, size: 5, totalElements: 0, totalPages: 0 });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    const cards = Array.from(el.querySelectorAll('.stat-value')).map((e) => e.textContent?.trim());
    expect(cards).toEqual(['$3,000.00', '$1,250.50', '$1,749.50', '58.3%']); // CurrencyPipe formatting

    // Only categories WITH a budget get a progress bar; Food is over (red), Transport is close (amber).
    expect(el.querySelectorAll('app-budget-bar').length).toBe(2);
    expect(el.querySelector('.progress.level-over')).not.toBeNull();
    expect(el.querySelector('.progress.level-warn')).not.toBeNull();
  });
});
