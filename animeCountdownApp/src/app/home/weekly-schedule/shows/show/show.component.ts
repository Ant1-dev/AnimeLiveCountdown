import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  OnInit,
  signal,
  computed,
} from '@angular/core';
import { Media } from '../../../../models/schedule.model';
import { interval } from 'rxjs';
import { MediaInfoService } from '../../../../services/media.info.service';
import { RouterLink } from '@angular/router';
import { SkeletonModule } from 'primeng/skeleton';
import { TimeRemaining } from '../../../../models/time-remaining.model';

@Component({
  selector: 'app-show',
  standalone: true,
  imports: [CommonModule, SkeletonModule, RouterLink],
  templateUrl: './show.component.html',
  styleUrl: './show.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShowComponent implements OnInit {
  media = input<Media>();
  renderPriority = input<'high' | 'medium' | 'low'>('low');

  private airingTime = signal<number | undefined>(undefined);
  timeLeft = signal<number | undefined>(undefined);
  private mediaInfoService = inject(MediaInfoService);
  private destroyRef = inject(DestroyRef);

  // Computed values for better performance
  timeRemaining = computed<TimeRemaining | null>(() => {
    const remainingTime = this.timeLeft();
    if (!remainingTime || remainingTime <= 0) return null;

    return {
      days: Math.floor(remainingTime / (1000 * 60 * 60 * 24)),
      hours: Math.floor(
        (remainingTime % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
      ),
      minutes: Math.floor((remainingTime % (1000 * 60 * 60)) / (1000 * 60)),
      seconds: Math.floor((remainingTime % (1000 * 60)) / 1000),
    };
  });

  // Computed value for title display
  displayTitle = computed(() => {
    const mediaData = this.media();
    return mediaData?.title_English || mediaData?.title_Romaji || '';
  });

  // Helper method to get WebP URL if WebP conversion is implemented
  getWebpUrl(url: string | undefined): string {
    if (!url) return '';
    // Convert the image URL to its WebP equivalent - adjust based on your setup
    return url.replace(/\.(jpg|jpeg|png)$/i, '.webp');
  }

  retrieveId(): void {
    const mediaData = this.media();
    if (mediaData) {
      this.mediaInfoService.getMediaId(mediaData.id);
    }
  }

  ngOnInit(): void {
    if (typeof window === 'undefined') {
      return;
    }

    const mediaData = this.media();
    if (!mediaData || !mediaData.next_Airing_At) {
      return;
    }

    try {
      const airingDate = new Date(mediaData.next_Airing_At);
      this.airingTime.set(airingDate.getTime());

      // Optimize update frequency based on time remaining to reduce CPU usage
      const getUpdateInterval = () => {
        const timeLeft = this.timeLeft();
        if (!timeLeft) return 1000;

        // If more than a day left, update every minute
        if (timeLeft > 24 * 60 * 60 * 1000) return 60000;
        // If more than an hour left, update every 15 seconds
        if (timeLeft > 60 * 60 * 1000) return 15000;
        // Otherwise update every second
        return 1000;
      };

      // Initial update
      this.updateTimeLeft();

      const subscription = interval(1000).subscribe(() => {
        this.updateTimeLeft();
      });

      this.destroyRef.onDestroy(() => {
        subscription.unsubscribe();
      });
    } catch (error) {
      console.error('Error processing date:', error);
    }
  }

  private updateTimeLeft(): void {
    const currentTime = Date.now();
    const airingTimeValue = this.airingTime();
    if (airingTimeValue && airingTimeValue > 0) {
      const remainingTime = airingTimeValue - currentTime;
      if (remainingTime > 0) {
        this.timeLeft.set(remainingTime);
      }
    }
  }
}
