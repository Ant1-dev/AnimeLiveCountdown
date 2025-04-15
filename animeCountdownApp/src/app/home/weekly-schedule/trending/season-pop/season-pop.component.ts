import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ScheduleService } from '../../../../services/schedule.service';
import { Media } from '../../../../models/schedule.model';
import { ShowComponent } from '../../shows/show/show.component';
import { MediaSkeletonComponent } from "../../../shared-home/media-skeleton/media-skeleton.component";

@Component({
  selector: 'app-season-pop',
  imports: [ShowComponent, MediaSkeletonComponent],
  templateUrl: './season-pop.component.html',
  styleUrl: './season-pop.component.css',
})
export class SeasonPopComponent implements OnInit {
  private scheduleService = inject(ScheduleService);
  private destroyRef = inject(DestroyRef);
  media = signal<Media[]>([]);
  error = signal<string>('');

  ngOnInit(): void {
    const subscription = this.scheduleService
      .renderMedia('trending', this.error())
      .subscribe({
        next: (media) => {
          if (media) {
            this.media.set(media);
          }
        },
        error: (error) => {
          this.error.set(error);
          console.log(error);
        },
      });

      this.destroyRef.onDestroy(() => {
        subscription.unsubscribe();
      });
  }
}
