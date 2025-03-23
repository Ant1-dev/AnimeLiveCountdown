import {
  Component,
  DestroyRef,
  inject,
  input,
  OnInit,
  signal,
} from '@angular/core';
import { MediaInfoService } from '../services/media.info.service';
import { MediaInfo } from '../models/media-info.model';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-media-info',
  imports: [CommonModule],
  templateUrl: './media-info.component.html',
  styleUrl: './media-info.component.css',
})
export class MediaInfoComponent implements OnInit {
  private mediaInfoService = inject(MediaInfoService);
  private destroyRef = inject(DestroyRef);
  public mediaDetails = signal<MediaInfo | undefined>(undefined);
  private error = signal<string>('');
  private route = inject(ActivatedRoute);

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        filter((params: ParamMap) => params.has('id')),
        switchMap((params: ParamMap) => {
          const id = Number(params.get('id'));
          this.mediaInfoService.getMediaId(id);
          return this.mediaInfoService.getMedia();
        })
      )
      .subscribe({
        next: (data) => {
          this.mediaDetails.set(data);
          console.log(data);
        },
        error: (err) =>
          this.error.set(err.message || 'An error occured info component'),
      });
  }
}
