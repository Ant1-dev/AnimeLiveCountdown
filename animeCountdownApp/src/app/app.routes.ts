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
        path: 'favorites',
        loadComponent: () => import('./favorites/favorites.component').then(m => m.FavoritesComponent),
        canActivate: [authGuard]
    },
];
