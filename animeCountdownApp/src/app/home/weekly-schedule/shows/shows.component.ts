import {
  Component,
  DestroyRef,
  inject,
  input,
  OnInit,
  signal,
} from '@angular/core';
import { ShowComponent } from './show/show.component';
import { CommonModule } from '@angular/common';
import { SkeletonModule } from 'primeng/skeleton';
import { Media } from '../../../models/schedule.model';
import { ScheduleService } from '../../../services/schedule.service';

type RenderPriority = 'high' | 'medium' | 'low';

@Component({
  selector: 'app-shows',
  imports: [ShowComponent, CommonModule, SkeletonModule],
  templateUrl: './shows.component.html',
  styleUrl: './shows.component.css',
})
export class ShowsComponent implements OnInit {
  weekDay = input<string>('');
  renderPriority = input<RenderPriority>('low');

  media = signal<Media[]>([]);
  isLoading = signal(false);
  error = signal<string>('');

  private scheduleService = inject(ScheduleService);
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.isLoading.set(true);

    const subscription = this.scheduleService
      .renderMedia(this.weekDay(), this.error())
      .subscribe({
        next: (media) => {
          if (media != undefined) {
            this.media.set(media);

            // Preload high priority images
            if (this.renderPriority() === 'high') {
              this.preloadImages(media.slice(0, 6));
            } else if (this.renderPriority() === 'medium') {
              this.preloadImages(media.slice(0, 3));
            }
          }
        },
        error: (error) => {
          console.log(error);
          this.error.set(error.message);
        },
        complete: () => {
          this.isLoading.set(false);
        },
      });

    this.destroyRef.onDestroy(() => {
      subscription.unsubscribe();
    });
  }

  // Determine show priority based on component priority and position
  getShowPriority(index: number): RenderPriority {
    if (this.renderPriority() === 'high') {
      return index < 8 ? 'high' : 'medium';
    } else if (this.renderPriority() === 'medium') {
      return index < 4 ? 'medium' : 'low';
    } else {
      return 'low';
    }
  }

  // Preload important images
  private preloadImages(media: Media[]): void {
    for (const show of media) {
      if (show.cover_Image_Url) {
        const img = new Image();
        img.src = show.cover_Image_Url;
      }
    }
  }
}
