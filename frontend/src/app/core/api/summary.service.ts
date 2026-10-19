import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Summary } from '../models';

/** Typed wrapper around GET /api/summary: all dashboard numbers for one month in one call. */
@Injectable({ providedIn: 'root' })
export class SummaryService {
  private readonly http = inject(HttpClient);

  get(month: string): Observable<Summary> {
    return this.http.get<Summary>('/api/summary', { params: { month } });
  }
}
