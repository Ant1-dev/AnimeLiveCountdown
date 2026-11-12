import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { IMAGE_CONFIG, IMAGE_LOADER } from '@angular/common';
import { IMAGE_WIDTHS, vercelImageLoader } from './shared/images';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';
import { routes } from './app.routes';
import {
  provideClientHydration,
  withEventReplay,
  withIncrementalHydration,
} from '@angular/platform-browser';
import {
  provideHttpClient,
  withFetch,
  withInterceptors,
} from '@angular/common/http';
import { AuthInterceptor } from './auth/auth.interceptor';
import { serverTimeoutInterceptor } from './seo/server-timeout.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withFetch(),
      withInterceptors([serverTimeoutInterceptor, AuthInterceptor])
    ),
    // Responsive, re-encoded AniList images (Vercel Image Optimization in production)
    { provide: IMAGE_LOADER, useValue: vercelImageLoader },
    { provide: IMAGE_CONFIG, useValue: { breakpoints: IMAGE_WIDTHS } },
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Aura,
        // Follow the site theme (data-theme on <html>, see ThemeService) instead of the OS setting
        options: { darkModeSelector: '[data-theme="dark"]' },
      },
    }),
    // Incremental hydration lets @defer blocks with hydrate triggers render on the server (for crawlers)
    // while their code still loads lazily in the browser.
    provideClientHydration(withEventReplay(), withIncrementalHydration()),
  ],
};
