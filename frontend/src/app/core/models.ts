/**
 * TypeScript interfaces that mirror the JSON returned by the Spring Boot API.
 *
 * Keeping them in one file means that if a backend DTO changes, updating the interface here makes
 * the compiler point at every component that needs fixing. Interfaces have no runtime cost: they
 * are erased when TypeScript compiles to JavaScript.
 *
 * Money note: the API sends amounts as JSON numbers (e.g. 54.2). JavaScript numbers are binary
 * floating point, so the frontend only DISPLAYS money (via CurrencyPipe) and never adds amounts up
 * itself; all totals are calculated exactly by the backend with BigDecimal.
 */

/** Mirrors the Java enum TransactionType. Used by transactions AND categories. */
export type TransactionType = 'INCOME' | 'EXPENSE';

export interface User {
  id: number;
  name: string;
  email: string;
}

/** Response of POST /api/auth/login and /api/auth/register. */
export interface AuthResponse {
  token: string;
  user: User;
}

/** A category as returned by GET /api/categories (mirrors CategoryResponse.java). */
export interface Category {
  id: number;
  name: string;
  color: string;            // "#RRGGBB"
  type: TransactionType;
  transactionCount: number; // > 0 means the category cannot be deleted
}

/** Body for creating/updating a category: everything except server-owned fields. */
export type CategoryRequest = Pick<Category, 'name' | 'color' | 'type'>;

/** A transaction (mirrors TransactionResponse.java). Dates are ISO strings: "2026-10-14". */
export interface Transaction {
  id: number;
  type: TransactionType;
  amount: number;           // always positive; `type` says whether it is + or -
  description: string;
  date: string;
  notes: string | null;
  category: { id: number; name: string; color: string };
  createdAt: string;
  updatedAt: string;
}

/** Body for POST/PUT /api/transactions (mirrors TransactionRequest.java). */
export interface TransactionRequest {
  type: TransactionType;
  amount: number;
  description: string;
  date: string;
  categoryId: number;
  notes: string | null;
}

/** A budget with progress (mirrors BudgetResponse.java). */
export interface Budget {
  id: number;
  categoryId: number;
  categoryName: string;
  color: string;
  month: string;            // "YYYY-MM"
  limit: number;
  spent: number;
  remaining: number;        // negative when over budget
  percentUsed: number;      // may exceed 100
}

/** Body for PUT /api/budgets (upsert). */
export interface BudgetRequest {
  categoryId: number;
  month: string;
  limit: number;
}

/** One slice of the "spending by category" doughnut. */
export interface CategorySpending {
  categoryId: number;
  name: string;
  color: string;
  spent: number;
  budget: number | null;      // null = no budget set for this category
  percentUsed: number | null;
}

/** Response of GET /api/summary?month=YYYY-MM (mirrors SummaryResponse.java). */
export interface Summary {
  month: string;
  totalIncome: number;
  totalExpense: number;
  balance: number;
  savingsRate: number;
  spendingByCategory: CategorySpending[];
  dailySpending: { date: string; amount: number }[];
  trend: { month: string; income: number; expense: number }[];
}

/** Generic page of results; mirrors the Java PageResponse<T> record. */
export interface Page<T> {
  content: T[];
  page: number;             // zero-based page index
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Shape of every error body produced by the backend's GlobalExceptionHandler. */
export interface ApiError {
  status: number;
  error: string;
  message: string;
  fieldErrors: Record<string, string>;
}
