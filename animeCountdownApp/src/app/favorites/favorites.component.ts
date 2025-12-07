import { Component, signal, inject, OnInit, DestroyRef, ChangeDetectionStrategy } from '@angular/core';
import { Media } from '../models/schedule.model';
import { FavoriteMediaService } from '../services/favorite.media.service';
import { AuthService } from '../auth/auth.service';
import { ShowComponent } from '../home/weekly-schedule/shows/show/show.component';
import { MediaSkeletonComponent } from '../home/shared-home/media-skeleton/media-skeleton.component';

type RenderPriority = 'high' | 'medium' | 'low';

@Component({
  selector: 'app-favorites',
  imports: [ShowComponent, MediaSkeletonComponent],
  templateUrl: './favorites.component.html',
  styleUrl: './favorites.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FavoritesComponent implements OnInit {
  media = signal<Media[]>([]);
  isLoading = signal(false);
  error = signal<string>('');

  private favMediaService = inject(FavoriteMediaService);
  private authService = inject(AuthService);
  private destroyRef = inject(DestroyRef);
  protected user = this.authService.user;

  ngOnInit(): void {
    if (this.user()) {
      this.loadFavorites();
    }
  }

  private loadFavorites(): void {
    this.isLoading.set(true);
    this.error.set('');

    const subscription = this.favMediaService
      .getFavoriteMedia(this.user()!.id)
      .subscribe({
        next: (favorites) => {
          this.media.set(favorites);
          this.isLoading.set(false);
        },
        error: (error) => {
          this.error.set('Failed to load favorites. Please try again later.');
          this.isLoading.set(false);
          console.error('Error loading favorites:', error);
        },
      });

    this.destroyRef.onDestroy(() => {
      subscription.unsubscribe();
    });
  }

  getShowPriority(index: number): RenderPriority {
    if (index < 8) {
      return 'high';
    } else if (index < 16) {
      return 'medium';
    }
    return 'low';
  }
}
