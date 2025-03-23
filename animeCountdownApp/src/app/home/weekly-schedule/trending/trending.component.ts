import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SkeletonModule } from 'primeng/skeleton';
import { Media } from '../../../models/schedule.model';
import { ScheduleService } from '../../../services/schedule.service';
import { ShowComponent } from '../shows/show/show.component';

@Component({
  selector: 'app-trending',
  standalone: true,
  imports: [CommonModule, ShowComponent, SkeletonModule],
  templateUrl: './trending.component.html',
  styleUrl: './trending.component.css',
})
export class TrendingComponent implements OnInit {
  media = signal<Media[]>([]);
  error = signal<string>('');
  isLoading = signal(true);

  private scheduleService = inject(ScheduleService);
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    const subscription = this.scheduleService
      .renderMedia('trending', this.error())
      .subscribe({
        next: (media) => {
          if (media != undefined) {
            const limitedMedia = media;
            this.media.set(limitedMedia);

            for (let i = 0; i < limitedMedia.length; i++) {
              if (limitedMedia[i].cover_Image_Url) {
                const img = new Image();
                img.src = limitedMedia[i].cover_Image_Url;
              }
            }
          }
          this.isLoading.set(false);
        },
        error: (error) => {
          this.error.set(error);
          console.log(error);
          this.isLoading.set(false);
        },
      });

    this.destroyRef.onDestroy(() => {
      subscription.unsubscribe();
    });
  }

  // Determine priority based on index
  getShowPriority(index: number): 'high' | 'medium' | 'low' {
    return index < 4 ? 'high' : 'medium';
  }
}
