import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page, Transaction, TransactionRequest, TransactionType } from '../models';
import { toParams } from './query-params';

/** Filters for the list endpoint. All optional; empty values are not sent. */
export interface TransactionQuery {
  month?: string;                  // "YYYY-MM"
  type?: TransactionType | '';
  categoryId?: number | null;
  q?: string;                      // description search
  page?: number;
  size?: number;
  sort?: string;                   // Spring Data format "property,direction", e.g. "amount,desc"
}

/**
 * Typed wrapper around /api/transactions.
 *
 * Components never build URLs; they call these methods. HttpClient returns "cold" Observables:
 * nothing is sent until something subscribes.
 *
 * URLs are relative ("/api/..."). In development the Angular dev server proxies them to the
 * backend on http://localhost:8082 (proxy.conf.json); in Docker, nginx does the same job.
 */
@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/transactions';

  list(query: TransactionQuery = {}): Observable<Page<Transaction>> {
    return this.http.get<Page<Transaction>>(this.baseUrl, { params: toParams(query) });
  }

  get(id: number): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.baseUrl}/${id}`);
  }

  create(body: TransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(this.baseUrl, body);
  }

  update(id: number, body: TransactionRequest): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.baseUrl}/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
