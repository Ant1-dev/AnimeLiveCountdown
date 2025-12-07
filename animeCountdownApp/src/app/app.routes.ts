import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';

export const routes: Routes = [
    {
        path: '',
        component: HomeComponent
    },
    {
        path: 'media/:id',
        loadComponent: () => import('./media-info/media-info.component').then(m => m.MediaInfoComponent)
    },
];
