import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { MediaInfoComponent } from './media-info/media-info.component';

export const routes: Routes = [
    {
        path: '',
        component: HomeComponent
    },
    {
        path: 'media/:id',
        component: MediaInfoComponent
    },
];
