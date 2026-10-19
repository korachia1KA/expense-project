import { Component, Input, computed, signal } from '@angular/core';
import { budgetLevel } from './budget-level';

/**
 * A reusable progress bar for budgets: <app-budget-bar [percent]="b.percentUsed" />
 *
 * A small STANDALONE component: it declares its own template and needs no NgModule. Any other
 * standalone component can use it by adding BudgetBarComponent to its `imports` array.
 *
 * The template is inline (`template:` instead of `templateUrl:`) because it's only a few lines.
 */
@Component({
  selector: 'app-budget-bar',
  standalone: true,
  template: `
    <div class="progress level-{{ level() }}" role="progressbar"
         [attr.aria-valuenow]="percentValue()" aria-valuemin="0" aria-valuemax="100">
      <div class="progress-fill" [style.width.%]="width()"></div>
    </div>
  `,
})
export class BudgetBarComponent {
  /**
   * @Input() receives a value from the parent template. We use a setter that writes into a signal
   * so the computed() values below update whenever the parent passes a new percentage.
   */
  @Input({ required: true }) set percent(value: number | null) {
    this.percentValue.set(value ?? 0);
  }

  protected readonly percentValue = signal(0);
  /** The bar never draws wider than 100 % even when spending is at 140 %. */
  protected readonly width = computed(() => Math.min(this.percentValue(), 100));
  protected readonly level = computed(() => budgetLevel(this.percentValue()));
}
