import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
    {
        path: '',
        component: HomeComponent
    },
    {
        path: 'media/:id',
        loadComponent: () => import('./media-info/media-info.component').then(m => m.MediaInfoComponent)
    },
    {
        path: 'following',
        loadComponent: () => import('./following/following.component').then(m => m.FollowingComponent),
        canActivate: [authGuard]
    },
    // Favorites were renamed to Following
    { path: 'favorites', redirectTo: 'following' },
];
