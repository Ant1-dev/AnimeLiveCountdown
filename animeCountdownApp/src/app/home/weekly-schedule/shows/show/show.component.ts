import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  inject,
  input,
  OnInit,
  output,
  signal,
} from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';
import { Media } from '../../../../schedule.model';
import { interval } from 'rxjs';
import { MediaInfoService } from '../../../../services/media.info.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-show',
  imports: [CommonModule, SkeletonModule, RouterLink],
  templateUrl: './show.component.html',
  styleUrl: './show.component.css',
})
export class ShowComponent implements OnInit {
  media = input<Media>();
  timeLeft = signal<number | undefined>(undefined);
  private airingTime: number | undefined;
  private mediaInfoService = inject(MediaInfoService);
  private destroyRef = inject(DestroyRef);


  retrieveId(event: MouseEvent): void {
    console.log("Selected", this.media()?.id);
    this.mediaInfoService.getMediaId(this.media()!.id);
  }

  ngOnInit(): void {
    if (typeof window === 'undefined') {
      return;
    }
    const mediaData = this.media();

    // Early return if no media data
    if (!mediaData) {
      console.log('No media data available');
      this.timeLeft.set(undefined);
      console.log('no data');
      return;
    }

    try {
      const airingDate = new Date(mediaData.next_Airing_At);
      this.airingTime = airingDate.getTime();

      const subscription = interval(1000).subscribe(() => {
        const currentTime = Date.now();

        if (this.airingTime && this.airingTime > 0) {
          const remainingTime = this.airingTime - currentTime;
          if (remainingTime > 0) {
            this.timeLeft.set(remainingTime);
          }
        }
      });

      this.destroyRef.onDestroy(() => {
        subscription.unsubscribe();
      });
    } catch (error) {
      console.error('Error processing date:', error);
    }
  }
}
