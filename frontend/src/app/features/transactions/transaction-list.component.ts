import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, merge } from 'rxjs';

import { CategoryService } from '../../core/api/category.service';
import { TransactionService } from '../../core/api/transaction.service';
import { Category, Page, Transaction, TransactionType } from '../../core/models';
import { currentMonth } from '../../shared/month';
import { errorMessage } from '../../shared/error-message';

type SortField = 'date' | 'amount' | 'description';

/**
 * The transactions table with SERVER-SIDE filtering, sorting and pagination.
 *
 * "Server-side" means the browser only downloads one page (20 rows); the database does the
 * filtering and sorting. That keeps the page fast even with years of history, unlike downloading
 * everything and filtering in the browser.
 *
 * Every filter is a FormControl, so we can listen to its valueChanges stream and reload.
 */
@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './transaction-list.component.html',
  styleUrl: './transaction-list.component.scss',
})
export class TransactionListComponent implements OnInit {
  private readonly api = inject(TransactionService);
  private readonly categoryApi = inject(CategoryService);
  private readonly destroyRef = inject(DestroyRef);

  // ---- Filters (FormControls). nonNullable: reset() returns to the initial value, not null.
  readonly month = new FormControl(currentMonth(), { nonNullable: true });   // '' = all time
  readonly type = new FormControl<TransactionType | ''>('', { nonNullable: true });
  readonly categoryId = new FormControl<number | null>(null);
  readonly search = new FormControl('', { nonNullable: true });

  // ---- Query state
  readonly pageIndex = signal(0);
  readonly pageSize = 20;
  readonly sortField = signal<SortField>('date');
  readonly sortDir = signal<'asc' | 'desc'>('desc');

  // ---- Results / UI state
  readonly page = signal<Page<Transaction> | null>(null);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  /** Id of the row showing "Delete? Yes / No" (inline confirmation instead of a browser popup). */
  readonly confirmingDeleteId = signal<number | null>(null);

  ngOnInit(): void {
    this.categoryApi.list().subscribe({ next: (c) => this.categories.set(c) });

    // Typing: wait until the user pauses for 300 ms, and skip if the text didn't actually change.
    const searchChanges = this.search.valueChanges.pipe(debounceTime(300), distinctUntilChanged());
    // merge() combines several streams into one: any filter change triggers a reload from page 1.
    // takeUntilDestroyed() unsubscribes automatically when the component is destroyed (no leaks).
    merge(searchChanges, this.month.valueChanges, this.type.valueChanges, this.categoryId.valueChanges)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload());

    this.load();
  }

  /** Back to page 1 (used whenever filters change) and fetch. */
  reload(): void {
    this.pageIndex.set(0);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api
      .list({
        month: this.month.value,
        type: this.type.value,
        categoryId: this.categoryId.value,
        q: this.search.value,
        page: this.pageIndex(),
        size: this.pageSize,
        sort: `${this.sortField()},${this.sortDir()}`,
      })
      .subscribe({
        next: (page) => {
          this.page.set(page);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(errorMessage(err));
          this.loading.set(false);
        },
      });
  }

  /** Clicking a header sorts by it; clicking it again flips the direction. */
  sortBy(field: SortField): void {
    if (this.sortField() === field) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortField.set(field);
      this.sortDir.set(field === 'description' ? 'asc' : 'desc');
    }
    this.reload();
  }

  sortIndicator(field: SortField): string {
    if (this.sortField() !== field) return '';
    return this.sortDir() === 'asc' ? '▲' : '▼';
  }

  goToPage(index: number): void {
    this.pageIndex.set(index);
    this.load();
  }

  delete(t: Transaction): void {
    this.api.delete(t.id).subscribe({
      next: () => {
        this.confirmingDeleteId.set(null);
        // If we just deleted the only row of a later page, step back one page.
        const p = this.page();
        if (p && p.content.length === 1 && this.pageIndex() > 0) {
          this.pageIndex.update((i) => i - 1);
        }
        this.load();
      },
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  clearFilters(): void {
    // emitEvent: false avoids four separate reloads; we reload once ourselves.
    this.month.setValue(currentMonth(), { emitEvent: false });
    this.type.setValue('', { emitEvent: false });
    this.categoryId.setValue(null, { emitEvent: false });
    this.search.setValue('', { emitEvent: false });
    this.reload();
  }

  get hasFilters(): boolean {
    return this.month.value !== currentMonth() || !!this.type.value || this.categoryId.value !== null || !!this.search.value;
  }
}
