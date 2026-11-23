import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { authGuard } from './auth/auth.guard';

const mediaPage = () => import('./media-info/media-info.component').then(m => m.MediaInfoComponent);
const seasonPage = () => import('./seasons/season-browser.component').then(m => m.SeasonBrowserComponent);

export const routes: Routes = [
    {
        path: '',
        component: HomeComponent
    },
    // Show pages. /anime/:id/:slug is canonical; the others redirect to it (301 on the server).
    { path: 'anime/:id/:slug', loadComponent: mediaPage },
    { path: 'anime/:id', loadComponent: mediaPage },
    { path: 'media/:id', loadComponent: mediaPage },
    // Season pages: /seasons is the current season, /seasons/2026/fall a specific one
    { path: 'seasons/:year/:season', loadComponent: seasonPage },
    { path: 'seasons', loadComponent: seasonPage },
    {
        path: 'schedule/:day',
        loadComponent: () => import('./schedule-day/schedule-day.component').then(m => m.ScheduleDayComponent)
    },
    {
        path: 'most-hyped',
        loadComponent: () => import('./most-hyped-page/most-hyped-page.component').then(m => m.MostHypedPageComponent)
    },
    {
        path: 'following',
        loadComponent: () => import('./following/following.component').then(m => m.FollowingComponent),
        canActivate: [authGuard]
    },
    {
        path: 'privacy',
        loadComponent: () => import('./legal/privacy-policy.component').then(m => m.PrivacyPolicyComponent)
    },
    {
        path: 'terms',
        loadComponent: () => import('./legal/terms.component').then(m => m.TermsComponent)
    },
    // Favorites were renamed to Following
    { path: 'favorites', redirectTo: 'following' },
    {
        path: 'notifications',
        loadComponent: () => import('./notification-settings/notification-settings.component').then(m => m.NotificationSettingsComponent),
        canActivate: [authGuard]
    },
    {
        // Works signed out too (picks are kept in the browser)
        path: 'my-services',
        loadComponent: () => import('./my-services/my-services.component').then(m => m.MyServicesComponent)
    },
    {
        path: '**',
        loadComponent: () => import('./not-found/not-found.component').then(m => m.NotFoundComponent)
    },
];
