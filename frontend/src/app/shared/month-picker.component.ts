import { Component, EventEmitter, Input, Output } from '@angular/core';
import { monthLabel, shiftMonth } from './month';

/**
 * "‹ October 2026 ›" control used on the dashboard and budgets pages.
 *
 * Communication pattern: the parent OWNS the month (data flows down through @Input) and is told
 * about changes through an @Output event (flows up). Usage:
 *   <app-month-picker [month]="month()" (monthChange)="month.set($event)" />
 * This "inputs down, events up" pattern keeps components reusable and predictable.
 */
@Component({
  selector: 'app-month-picker',
  standalone: true,
  template: `
    <div class="picker">
      <button class="btn btn-secondary btn-sm" type="button" (click)="go(-1)" aria-label="Previous month">‹</button>
      <span class="label">{{ label }}</span>
      <button class="btn btn-secondary btn-sm" type="button" (click)="go(1)" aria-label="Next month">›</button>
    </div>
  `,
  styles: `
    .picker { display: inline-flex; align-items: center; gap: 8px; }
    .label { min-width: 130px; text-align: center; font-weight: 600; }
  `,
})
export class MonthPickerComponent {
  @Input({ required: true }) month!: string;

  /** EventEmitter is how a child component raises a custom event for its parent. */
  @Output() readonly monthChange = new EventEmitter<string>();

  get label(): string {
    return monthLabel(this.month);
  }

  go(delta: number): void {
    this.monthChange.emit(shiftMonth(this.month, delta));
  }
}
