// show.component.ts
import { CommonModule, NgOptimizedImage } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  OnInit,
  computed,
} from '@angular/core';
import { Media } from '../../../../models/schedule.model';
import { MediaInfoService } from '../../../../services/media.info.service';
import { MediaTimeService } from '../../../../services/media-time.service';
import { SkeletonModule } from 'primeng/skeleton';
import { TimeRemaining } from '../../../../models/time-remaining.model';
import { MatIcon } from '@angular/material/icon';
import { TooltipModule, Tooltip } from 'primeng/tooltip';
import { FavoriteMediaService } from '../../../../services/favorite.media.service';
import { AuthService } from '../../../../auth/auth.service';

@Component({
  selector: 'app-show',
  imports: [CommonModule, SkeletonModule, MatIcon, TooltipModule, Tooltip, NgOptimizedImage],
  templateUrl: './show.component.html',
  styleUrl: './show.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShowComponent implements OnInit {
  media = input<Media>();
  renderPriority = input<'high' | 'medium' | 'low'>('low');
  // If true, show "Airing Now" when countdown finishes. If false, hide card completely.
  showAiringNow = input<boolean>(false);

  private mediaInfoService = inject(MediaInfoService);
  private mediaTimeService = inject(MediaTimeService);
  private favMediaService = inject(FavoriteMediaService);
  private authService = inject(AuthService);

  protected user = this.authService.user;

  timeRemaining = computed<TimeRemaining | null>(() => {
    const mediaData = this.media();
    if (!mediaData || !mediaData.id) return null;
    return this.mediaTimeService.getTimeRemaining(mediaData.id);
  });

  isAiringNow = computed(() => {
    return this.timeRemaining() === null && this.showAiringNow();
  });

  getDisplayTitle(): string {
    return this.mediaTimeService.getDisplayTitle(this.media());
  }

  retrieveId(): void {
    const mediaData = this.media();
    if (mediaData) {
      this.mediaInfoService.getMediaId(mediaData.id);
    }
  }

  saveMedia(): void {
    if (this.user()) {
      this.favMediaService.addFavoriteMedia(this.user()!.id, this.media()!.id).subscribe({
        next: (res) => console.log(res),
        error: (error) => console.log(error)
      });
      console.log("Media was added");
    }
  }

  ngOnInit(): void {
    this.mediaTimeService.initializeTimer(this.media());
  }

  tooltipOptions = {
    showDelay: 150,
    autoHide: false,
  };
}
