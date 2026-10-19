import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';

/**
 * Root component: the app "shell". It renders the top navigation bar and a <router-outlet>,
 * the placeholder where the component for the current URL is displayed.
 *
 * `standalone: true` means the component declares its own dependencies in `imports`
 * (RouterOutlet, RouterLink...) instead of relying on an NgModule.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  /** `protected` = usable in this component's template, but not from other TypeScript code. */
  protected readonly auth = inject(AuthService);
}
