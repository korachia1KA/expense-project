import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

/**
 * Browser entry point. Angular starts here: it creates the root component (AppComponent),
 * renders it into the <app-root> tag of index.html, and registers the providers from appConfig.
 * There is no NgModule: this is a "standalone" Angular application.
 */
bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
