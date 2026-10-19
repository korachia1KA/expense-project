import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Chart, registerables } from 'chart.js';

import { SummaryService } from '../../core/api/summary.service';
import { TransactionService } from '../../core/api/transaction.service';
import { Summary, Transaction } from '../../core/models';
import { BudgetBarComponent } from '../../shared/budget-bar.component';
import { budgetLevel } from '../../shared/budget-level';
import { MonthPickerComponent } from '../../shared/month-picker.component';
import { currentMonth, monthLabel } from '../../shared/month';
import { errorMessage } from '../../shared/error-message';

// Chart.js is tree-shakable: chart types, scales and plugins must be registered before use.
// `registerables` registers all of them (simplest; costs a few extra KB in the dashboard chunk only).
Chart.register(...registerables);

const INCOME_COLOR = '#16a34a';
const EXPENSE_COLOR = '#dc2626';

/** Compact currency formatter for chart axes and tooltips: 1200 → "$1,200". */
const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });

/**
 * Dashboard: month picker, four summary cards, three charts, budget progress and recent activity.
 *
 * Everything comes from TWO requests made in parallel: GET /api/summary (all the numbers, already
 * aggregated by the backend) and GET /api/transactions (the 5 latest of the month).
 *
 * Lifecycle hooks used:
 *  - ngOnInit:        component created → start loading data
 *  - ngAfterViewInit: template rendered → <canvas> elements may now exist for Chart.js
 *  - ngOnDestroy:     component removed → destroy charts to free memory and event listeners
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, DecimalPipe, MonthPickerComponent, BudgetBarComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly summaryApi = inject(SummaryService);
  private readonly transactionApi = inject(TransactionService);

  // @ViewChild gives the class a reference to an element marked #name in the template.
  // The setters re-render charts whenever Angular creates (or re-creates) a canvas inside an @if block.
  @ViewChild('categoryChart') set categoryCanvas(ref: ElementRef<HTMLCanvasElement> | undefined) {
    this.canvases.category = ref?.nativeElement;
  }
  @ViewChild('trendChart') set trendCanvas(ref: ElementRef<HTMLCanvasElement> | undefined) {
    this.canvases.trend = ref?.nativeElement;
  }
  @ViewChild('dailyChart') set dailyCanvas(ref: ElementRef<HTMLCanvasElement> | undefined) {
    this.canvases.daily = ref?.nativeElement;
  }

  readonly month = signal(currentMonth());
  readonly summary = signal<Summary | null>(null);
  readonly recent = signal<Transaction[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  /** computed(): derived state that updates automatically when `summary` changes. */
  readonly budgeted = computed(() => (this.summary()?.spendingByCategory ?? []).filter((c) => c.budget !== null));
  readonly hasActivity = computed(() => {
    const s = this.summary();
    return !!s && (s.totalIncome > 0 || s.totalExpense > 0);
  });
  readonly monthName = computed(() => monthLabel(this.month()));

  /** Exposed to the template so it can color budget text. */
  readonly budgetLevel = budgetLevel;

  // Each chart has a different generic type (Chart<'bar'>, Chart<'doughnut'>...). We only ever
  // call destroy(), so storing them as Pick<Chart, 'destroy'> avoids fighting the generics.
  private charts: Pick<Chart, 'destroy'>[] = [];
  private canvases: { category?: HTMLCanvasElement; trend?: HTMLCanvasElement; daily?: HTMLCanvasElement } = {};
  private viewReady = false;

  ngOnInit(): void {
    this.load();
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.renderCharts();
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  /** Called by the month picker's (monthChange) event. */
  changeMonth(month: string): void {
    this.month.set(month);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    // forkJoin runs both requests in parallel and emits once, when BOTH have completed.
    forkJoin({
      summary: this.summaryApi.get(this.month()),
      recent: this.transactionApi.list({ month: this.month(), page: 0, size: 5 }),
    }).subscribe({
      next: ({ summary, recent }) => {
        this.summary.set(summary);
        this.recent.set(recent.content);
        this.loading.set(false);
        this.renderCharts();
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  /**
   * Draws the charts once BOTH the data has arrived AND the view exists. Either can happen first,
   * so both call this method. setTimeout waits one tick so @if blocks have rendered their <canvas>.
   */
  private renderCharts(): void {
    const s = this.summary();
    if (!s || !this.viewReady) return;
    setTimeout(() => {
      this.destroyCharts();
      const { category, trend, daily } = this.canvases;
      if (category) this.charts.push(this.buildCategoryChart(category, s));
      if (trend) this.charts.push(this.buildTrendChart(trend, s));
      if (daily) this.charts.push(this.buildDailyChart(daily, s));
    });
  }

  private destroyCharts(): void {
    this.charts.forEach((c) => c.destroy());
    this.charts = [];
  }

  /** Doughnut: one slice per expense category, in the category's own color. */
  private buildCategoryChart(canvas: HTMLCanvasElement, s: Summary) {
    const rows = s.spendingByCategory.filter((c) => c.spent > 0);
    return new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: rows.map((c) => c.name),
        datasets: [{ data: rows.map((c) => c.spent), backgroundColor: rows.map((c) => c.color), borderWidth: 2 }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '62%',
        plugins: {
          legend: { position: 'right' },
          tooltip: { callbacks: { label: (ctx) => ` ${ctx.label}: ${money.format(ctx.parsed)}` } },
        },
      },
    });
  }

  /** Grouped bars: income vs expense for the last 6 months. */
  private buildTrendChart(canvas: HTMLCanvasElement, s: Summary) {
    return new Chart(canvas, {
      type: 'bar',
      data: {
        labels: s.trend.map((t) => monthLabel(t.month, 'short')),
        datasets: [
          { label: 'Income', data: s.trend.map((t) => t.income), backgroundColor: INCOME_COLOR, borderRadius: 6, maxBarThickness: 28 },
          { label: 'Expenses', data: s.trend.map((t) => t.expense), backgroundColor: EXPENSE_COLOR, borderRadius: 6, maxBarThickness: 28 },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'bottom' } },
        scales: {
          y: { beginAtZero: true, ticks: { callback: (v) => money.format(Number(v)) } },
          x: { grid: { display: false } },
        },
      },
    });
  }

  /** Line: spending on each day of the selected month (the backend already filled empty days with 0). */
  private buildDailyChart(canvas: HTMLCanvasElement, s: Summary) {
    return new Chart(canvas, {
      type: 'line',
      data: {
        // "2026-10-07" → "7": the day of month is enough on this axis.
        labels: s.dailySpending.map((d) => String(Number(d.date.slice(8, 10)))),
        datasets: [{
          label: 'Spent',
          data: s.dailySpending.map((d) => d.amount),
          borderColor: '#0f766e',
          backgroundColor: 'rgba(15, 118, 110, 0.12)',
          fill: true,
          tension: 0.3,
          pointRadius: 2,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: { beginAtZero: true, ticks: { callback: (v) => money.format(Number(v)) } },
          x: { grid: { display: false } },
        },
      },
    });
  }
}
