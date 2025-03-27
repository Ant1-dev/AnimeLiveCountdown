// show.component.ts
import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  OnInit,
  signal,
  computed
} from '@angular/core';
import { Media } from '../../../../models/schedule.model';
import { MediaInfoService } from '../../../../services/media.info.service';
import { MediaTimeService } from '../../../../services/media-time.service';
import { SkeletonModule } from 'primeng/skeleton';
import { TimeRemaining } from '../../../../models/time-remaining.model';

@Component({
  selector: 'app-show',
  imports: [CommonModule, SkeletonModule],
  templateUrl: './show.component.html',
  styleUrl: './show.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShowComponent implements OnInit {
  media = input<Media>();
  renderPriority = input<'high' | 'medium' | 'low'>('low');
  
  private mediaInfoService = inject(MediaInfoService);
  mediaTimeService = inject(MediaTimeService);
  
  timeRemaining = computed<TimeRemaining | null>(() => {
    const mediaData = this.media();
    if (!mediaData || !mediaData.id) return null;
    return this.mediaTimeService.getTimeRemaining(mediaData.id);
  });
  
  getDisplayTitle(): string {
    return this.mediaTimeService.getDisplayTitle(this.media());
  }
  
  getWebpUrl(url: string | undefined): string {
    return this.mediaTimeService.getWebpUrl(url);
  }
  
  retrieveId(): void {
    const mediaData = this.media();
    if (mediaData) {
      this.mediaInfoService.getMediaId(mediaData.id);
    }
  }
  
  ngOnInit(): void {
    this.mediaTimeService.initializeTimer(this.media());
  }
}