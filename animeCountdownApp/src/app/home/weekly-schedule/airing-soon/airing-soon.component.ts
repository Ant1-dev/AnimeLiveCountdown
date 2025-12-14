import { Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from '@angular/core';
import { Media } from '../../../models/schedule.model';
import { ScheduleService } from '../../../services/schedule.service';
import { ShowComponent } from '../shows/show/show.component';
import { MediaSkeletonComponent } from "../../shared-home/media-skeleton/media-skeleton.component";
import { timer, switchMap } from 'rxjs';

@Component({
  selector: 'app-airing-soon',
  imports: [ShowComponent, MediaSkeletonComponent],
  templateUrl: './airing-soon.component.html',
  styleUrl: './airing-soon.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiringSoonComponent implements OnInit {
  media = signal<Media[]>([]);
  isLoading = signal(false);
  error = signal<string>('');
  private scheduleService = inject(ScheduleService);
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.isLoading.set(true);

    // Start immediately (0ms), then poll every 60 seconds (60000ms)
    const subscription = timer(0, 60000)
      .pipe(
        switchMap(() => this.scheduleService.renderMedia('soon', this.error()))
      )
      .subscribe({
        next: (media) => {
          if (media != undefined) {
            this.media.set(media);
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
}
