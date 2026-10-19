import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Budget, BudgetRequest } from '../models';

/** Typed wrapper around /api/budgets. */
@Injectable({ providedIn: 'root' })
export class BudgetService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/budgets';

  /** Budgets of one month ("YYYY-MM"), each with spent / remaining / percentUsed. */
  list(month: string): Observable<Budget[]> {
    return this.http.get<Budget[]>(this.baseUrl, { params: { month } });
  }

  /** Create OR update the budget for (category, month): the backend "upserts". */
  save(body: BudgetRequest): Observable<Budget> {
    return this.http.put<Budget>(this.baseUrl, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
