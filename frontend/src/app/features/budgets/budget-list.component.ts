import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { BudgetService } from '../../core/api/budget.service';
import { CategoryService } from '../../core/api/category.service';
import { SummaryService } from '../../core/api/summary.service';
import { Budget, Category } from '../../core/models';
import { BudgetBarComponent } from '../../shared/budget-bar.component';
import { budgetLevel } from '../../shared/budget-level';
import { MonthPickerComponent } from '../../shared/month-picker.component';
import { currentMonth, monthLabel } from '../../shared/month';
import { errorMessage } from '../../shared/error-message';

/** One row of the page: an expense category, its budget (if any) and what was spent. */
interface BudgetRow {
  category: Category;
  budget: Budget | null;
  spent: number;
}

/**
 * Budgets page: for the selected month, every EXPENSE category is listed with its limit and
 * progress. Limits are edited inline; saving calls PUT /api/budgets, which creates OR updates
 * (upserts) the budget, so this page never needs to know whether one already existed.
 *
 * Data comes from three parallel requests: categories, budgets of the month (with spent/percent),
 * and the month summary (spending of categories that have no budget yet).
 */
@Component({
  selector: 'app-budget-list',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe, DecimalPipe, MonthPickerComponent, BudgetBarComponent],
  templateUrl: './budget-list.component.html',
  styleUrl: './budget-list.component.scss',
})
export class BudgetListComponent implements OnInit {
  private readonly budgetApi = inject(BudgetService);
  private readonly categoryApi = inject(CategoryService);
  private readonly summaryApi = inject(SummaryService);

  readonly month = signal(currentMonth());
  readonly rows = signal<BudgetRow[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  /** Category whose limit is being edited inline (null = none). */
  readonly editingCategoryId = signal<number | null>(null);
  readonly confirmingRemoveId = signal<number | null>(null);
  readonly saving = signal(false);
  /** A single FormControl is enough: only one row is edited at a time. */
  readonly limit = new FormControl<number | null>(null, [Validators.required, Validators.min(0.01)]);

  readonly monthName = computed(() => monthLabel(this.month()));
  /** Totals across budgeted categories, for the header card. */
  readonly totals = computed(() => {
    const budgeted = this.rows().filter((r) => r.budget);
    return {
      limit: budgeted.reduce((sum, r) => sum + r.budget!.limit, 0),
      spent: budgeted.reduce((sum, r) => sum + r.budget!.spent, 0),
      count: budgeted.length,
    };
  });

  readonly budgetLevel = budgetLevel;

  ngOnInit(): void {
    this.load();
  }

  changeMonth(month: string): void {
    this.month.set(month);
    this.cancelEdit();
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      categories: this.categoryApi.list(),
      budgets: this.budgetApi.list(this.month()),
      summary: this.summaryApi.get(this.month()),
    }).subscribe({
      next: ({ categories, budgets, summary }) => {
        // Maps make the joins below O(1) lookups instead of nested loops.
        const budgetByCategory = new Map(budgets.map((b) => [b.categoryId, b]));
        const spentByCategory = new Map(summary.spendingByCategory.map((s) => [s.categoryId, s.spent]));
        this.rows.set(
          categories
            .filter((c) => c.type === 'EXPENSE')   // budgets only apply to expenses
            .map((c) => ({
              category: c,
              budget: budgetByCategory.get(c.id) ?? null,
              spent: spentByCategory.get(c.id) ?? 0,
            }))
            // Budgeted categories first, then alphabetical.
            .sort((a, b) => Number(!!b.budget) - Number(!!a.budget) || a.category.name.localeCompare(b.category.name)),
        );
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  startEdit(row: BudgetRow): void {
    this.editingCategoryId.set(row.category.id);
    this.limit.reset(row.budget?.limit ?? null);
  }

  cancelEdit(): void {
    this.editingCategoryId.set(null);
    this.limit.reset(null);
  }

  save(row: BudgetRow): void {
    if (this.limit.invalid) {
      this.limit.markAsTouched();
      return;
    }
    this.saving.set(true);
    this.budgetApi.save({ categoryId: row.category.id, month: this.month(), limit: Number(this.limit.value) }).subscribe({
      next: () => {
        this.saving.set(false);
        this.cancelEdit();
        this.load();
      },
      error: (err) => {
        this.error.set(errorMessage(err, 'Could not save the budget'));
        this.saving.set(false);
      },
    });
  }

  remove(budget: Budget): void {
    this.budgetApi.delete(budget.id).subscribe({
      next: () => {
        this.confirmingRemoveId.set(null);
        this.load();
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
  }
}
