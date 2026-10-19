import { Component, DestroyRef, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';

import { CategoryService } from '../../core/api/category.service';
import { TransactionService } from '../../core/api/transaction.service';
import { Category, TransactionRequest, TransactionType } from '../../core/models';
import { errorMessage } from '../../shared/error-message';
import { todayIso } from '../../shared/month';

/**
 * One component serves both "create" (/transactions/new) and "edit" (/transactions/:id/edit).
 * If the router provides an `id` input we are editing; otherwise we are creating.
 *
 * The validators mirror TransactionRequest.java so the user gets instant feedback, while the
 * backend remains the real authority (it re-validates everything).
 */
@Component({
  selector: 'app-transaction-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './transaction-form.component.html',
})
export class TransactionFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(TransactionService);
  private readonly categoryApi = inject(CategoryService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /** Filled from the ":id" route parameter (withComponentInputBinding in app.config.ts). */
  @Input() id?: string;

  readonly maxDate = todayIso();

  readonly form = this.fb.group({
    type: this.fb.nonNullable.control<TransactionType>('EXPENSE'),
    // min 0.01 = "greater than zero"; the pattern allows at most 2 decimals (cents).
    amount: this.fb.control<number | null>(null, [
      Validators.required, Validators.min(0.01), Validators.pattern(/^\d{1,10}(\.\d{1,2})?$/),
    ]),
    description: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(255)]),
    date: this.fb.nonNullable.control(todayIso(), Validators.required),
    categoryId: this.fb.control<number | null>(null, Validators.required),
    notes: this.fb.nonNullable.control('', Validators.maxLength(1000)),
  });

  readonly categories = signal<Category[]>([]);
  readonly saving = signal(false);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  /**
   * toSignal() converts an RxJS Observable into a signal. Here: the currently selected type.
   * `initialValue` is used until the first valueChanges emission.
   */
  readonly selectedType = toSignal(this.form.controls.type.valueChanges, { initialValue: this.form.controls.type.value });

  /** Only categories whose type matches the transaction type (the backend enforces the same rule). */
  readonly categoryOptions = computed(() => this.categories().filter((c) => c.type === this.selectedType()));

  get isEdit(): boolean {
    return !!this.id;
  }

  ngOnInit(): void {
    this.categoryApi.list().subscribe({
      next: (c) => this.categories.set(c),
      error: (err) => this.error.set(errorMessage(err, 'Could not load categories')),
    });

    // Switching Expense ↔ Income clears a category that no longer fits.
    this.form.controls.type.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((type) => {
      const selected = this.categories().find((c) => c.id === this.form.controls.categoryId.value);
      if (selected && selected.type !== type) {
        this.form.controls.categoryId.setValue(null);
      }
    });

    if (this.id) {
      this.loading.set(true);
      this.api.get(Number(this.id)).subscribe({
        next: (t) => {
          // patchValue copies matching keys into the form controls.
          this.form.patchValue({
            type: t.type,
            amount: t.amount,
            description: t.description,
            date: t.date,
            categoryId: t.category.id,
            notes: t.notes ?? '',
          });
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(errorMessage(err, 'Could not load this transaction'));
          this.loading.set(false);
        },
      });
    }
  }

  setType(type: TransactionType): void {
    this.form.controls.type.setValue(type);
  }

  /** True when a control should display its error message (invalid AND the user has touched it). */
  showError(name: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);

    const v = this.form.getRawValue();
    const body: TransactionRequest = {
      type: v.type,
      amount: Number(v.amount),
      description: v.description,
      date: v.date,
      categoryId: v.categoryId!,   // "!" is safe: Validators.required guarantees a value here
      notes: v.notes || null,
    };
    const request$ = this.id ? this.api.update(Number(this.id), body) : this.api.create(body);

    request$.subscribe({
      next: () => this.router.navigate(['/transactions']),
      error: (err) => {
        this.error.set(errorMessage(err, 'Could not save'));
        this.saving.set(false);
      },
    });
  }
}
