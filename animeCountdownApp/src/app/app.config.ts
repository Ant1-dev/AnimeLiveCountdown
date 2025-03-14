import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';
import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';

// Import the analytics function
import { inject as injectVercelAnalytics } from '@vercel/analytics';

// Create a provider function for Vercel Analytics
function provideVercelAnalytics() {
  return {
    provide: 'VERCEL_ANALYTICS',
    useFactory: () => {
      const doc = inject(DOCUMENT);
      // Only inject analytics in browser environment
      if (typeof window !== 'undefined') {
        injectVercelAnalytics();
        return true;
      }
      return false;
    },
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideClientHydration(),
    provideHttpClient(withFetch()),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: Aura,
      },
    }),
    // Add Vercel Analytics provider
    provideVercelAnalytics(),
  ],
};