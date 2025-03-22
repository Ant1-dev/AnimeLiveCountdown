import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { Media } from '../../../schedule.model';
import { ScheduleService } from '../../../services/schedule.service';
import { ShowComponent } from '../shows/show/show.component';

@Component({
  selector: 'app-airing-soon',
  imports: [ShowComponent],
  templateUrl: './airing-soon.component.html',
  styleUrl: './airing-soon.component.css',
})
export class AiringSoonComponent implements OnInit {
  media = signal<Media[]>([]);
  isLoading = signal(false);
  error = signal<string>('');
  private scheduleService = inject(ScheduleService);
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.isLoading.set(true);

    const subscription = this.scheduleService
      .renderMedia('soon', this.error())
      .subscribe({
        next: (media) => {
          if (media != undefined) {
            this.media.set(media);
          }
        },
        error: (error) => {
          this.error.set(error);
          console.log(error);
        },
        complete: () => {
          this.isLoading.set(false);
        },
      });

    this.destroyRef.onDestroy(() => {
      subscription.unsubscribe();
    });
  }
}
