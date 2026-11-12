import { Component, inject, input, ChangeDetectionStrategy } from '@angular/core';
import { Media } from '../../../models/schedule.model';
import { CommonModule } from '@angular/common';
import { MediaInfoService } from '../../../services/media.info.service';
import { MediaInfo } from '../../../models/media.info.model';
import { AnimePathPipe } from '../../../seo/anime-path.pipe';
import { OptimizedImagePipe } from '../../../shared/optimized-image.pipe';

@Component({
  selector: 'app-result',
  imports: [OptimizedImagePipe, AnimePathPipe, CommonModule],
  templateUrl: './result.component.html',
  styleUrl: './result.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResultComponent {
  result = input<MediaInfo>();
  mediaInfoServe = inject(MediaInfoService);

  retrieveId() {
    this.mediaInfoServe.getMediaId(this.result()!.id);
  }
}
