import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../shared/error-message';

/** Credentials of the account created by the backend's DemoDataSeeder. */
export const DEMO_EMAIL = 'demo@example.com';
export const DEMO_PASSWORD = 'password123';

/**
 * Login page, built with a REACTIVE FORM.
 *
 * Reactive forms are defined in TypeScript (not in the HTML): the form model below is the single
 * source of truth, which makes validation easy to read, test and reason about. The template binds
 * to it with [formGroup] and formControlName.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './auth-page.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** nonNullable: reset() returns to '' instead of null, which keeps the value types simple (string). */
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  /** UI state as signals: the template re-renders automatically when they change. */
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  /** Fills in the seeded demo account and logs in, handy for recruiters trying the app. */
  useDemo(): void {
    this.form.setValue({ email: DEMO_EMAIL, password: DEMO_PASSWORD });
    this.submit();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched(); // reveal the validation message under every field
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    const { email, password } = this.form.getRawValue();
    // Nothing is sent until we subscribe. next = success, error = HTTP error (e.g. 401).
    this.auth.login(email, password).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.error.set(errorMessage(err, 'Login failed'));
        this.loading.set(false);
      },
    });
  }
}
