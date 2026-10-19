import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Category, CategoryRequest } from '../models';

/** Typed wrapper around /api/categories (CRUD). */
@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/categories';

  /** All categories of the logged-in user (not paginated: there are only a handful). */
  list(): Observable<Category[]> {
    return this.http.get<Category[]>(this.baseUrl);
  }

  create(body: CategoryRequest): Observable<Category> {
    return this.http.post<Category>(this.baseUrl, body);
  }

  update(id: number, body: CategoryRequest): Observable<Category> {
    return this.http.put<Category>(`${this.baseUrl}/${id}`, body);
  }

  /** Fails with 409 Conflict when transactions still use the category. */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
