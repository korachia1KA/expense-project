import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CategoryService } from '../../core/api/category.service';
import { Category, CategoryRequest, TransactionType } from '../../core/models';
import { errorMessage } from '../../shared/error-message';

/** A few pleasant colors offered for new categories (users can still pick any color). */
const PALETTE = ['#f97316', '#0ea5e9', '#8b5cf6', '#eab308', '#ec4899', '#14b8a6', '#6366f1', '#22c55e', '#64748b'];

/**
 * Categories page: list (grouped into expense and income), plus one form that is used both to
 * add a new category and to edit an existing one (`editingId` tells which mode we're in).
 *
 * Deleting a category that still has transactions is refused by the backend with 409 Conflict;
 * we simply show the backend's explanatory message.
 */
@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './category-list.component.html',
  styleUrl: './category-list.component.scss',
})
export class CategoryListComponent implements OnInit {
  private readonly api = inject(CategoryService);
  private readonly fb = inject(FormBuilder);

  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  /** null = the form adds a new category; a number = the form edits that category. */
  readonly editingId = signal<number | null>(null);
  readonly confirmingDeleteId = signal<number | null>(null);

  readonly expenseCategories = computed(() => this.categories().filter((c) => c.type === 'EXPENSE'));
  readonly incomeCategories = computed(() => this.categories().filter((c) => c.type === 'INCOME'));
  /** The two lists shown on the page, so the template can render them with one @for. */
  readonly groups = computed(() => [
    { title: 'Expense categories', items: this.expenseCategories() },
    { title: 'Income categories', items: this.incomeCategories() },
  ]);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(50)]],
    color: [PALETTE[0], [Validators.required, Validators.pattern(/^#[0-9a-fA-F]{6}$/)]],
    type: ['EXPENSE' as TransactionType, Validators.required],
  });

  readonly palette = PALETTE;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.list().subscribe({
      next: (c) => {
        this.categories.set(c);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  /** Puts a category's values into the form and switches it to "edit" mode. */
  edit(c: Category): void {
    this.editingId.set(c.id);
    this.form.setValue({ name: c.name, color: c.color, type: c.type });
    this.error.set(null);
  }

  cancelEdit(): void {
    this.editingId.set(null);
    // Suggest the next palette color for the next category.
    this.form.reset({ name: '', color: PALETTE[this.categories().length % PALETTE.length], type: 'EXPENSE' });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    const body: CategoryRequest = this.form.getRawValue();
    const id = this.editingId();
    const request$ = id === null ? this.api.create(body) : this.api.update(id, body);
    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.cancelEdit();
        this.load();
      },
      error: (err) => {
        this.error.set(errorMessage(err, 'Could not save the category'));
        this.saving.set(false);
      },
    });
  }

  delete(c: Category): void {
    this.api.delete(c.id).subscribe({
      next: () => {
        this.confirmingDeleteId.set(null);
        if (this.editingId() === c.id) this.cancelEdit();
        this.load();
      },
      error: (err) => {
        // e.g. 409: "Category 'Food' is used by 12 transactions. Move or delete those transactions first."
        this.error.set(errorMessage(err, 'Could not delete the category'));
        this.confirmingDeleteId.set(null);
      },
    });
  }
}
