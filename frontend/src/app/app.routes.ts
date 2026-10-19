import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';

/**
 * URL → component mapping.
 *
 * `loadComponent` LAZY-LOADS each page: its code goes into a separate JavaScript "chunk" that is
 * downloaded only when the user first visits that URL. The login page therefore doesn't make the
 * user download Chart.js, which only the dashboard needs.
 *
 * `canActivate` runs guards before entering a route (see core/auth/auth.guard.ts).
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },

  // Public pages (guestGuard sends already-logged-in users to the dashboard)
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register.component').then((m) => m.RegisterComponent),
  },

  // Private pages (authGuard sends anonymous users to /login)
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'transactions',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/transactions/transaction-list.component').then((m) => m.TransactionListComponent),
  },
  {
    path: 'transactions/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/transactions/transaction-form.component').then((m) => m.TransactionFormComponent),
  },
  {
    // ":id" becomes the `id` @Input of TransactionFormComponent (thanks to withComponentInputBinding)
    path: 'transactions/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/transactions/transaction-form.component').then((m) => m.TransactionFormComponent),
  },
  {
    path: 'categories',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/categories/category-list.component').then((m) => m.CategoryListComponent),
  },
  {
    path: 'budgets',
    canActivate: [authGuard],
    loadComponent: () => import('./features/budgets/budget-list.component').then((m) => m.BudgetListComponent),
  },

  // Unknown URLs go home
  { path: '**', redirectTo: 'dashboard' },
];
