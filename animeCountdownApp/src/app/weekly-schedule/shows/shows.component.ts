import { Component, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { ShowComponent } from "./show/show.component";
import { Media } from '../../schedule.model';
import { ScheduleService } from '../../schedule.service';

@Component({
    selector: 'app-shows',
    imports: [ShowComponent],
    templateUrl: './shows.component.html',
    styleUrl: './shows.component.css'
})
export class ShowsComponent implements OnInit {
    weekDay = input<string>('');
    media = signal<Media[]>([]);
    isLoading = signal(false);
    error = signal<string>('');
    private scheduleService = inject(ScheduleService);
    private destroyRef = inject(DestroyRef);

    ngOnInit(): void {
     this.isLoading.set(true);
     const subscription = this.scheduleService
        .renderWeekdayMedia(this.weekDay(), this.error())
        .subscribe({
            next: (media) => {
                if (media != undefined) {
                    this.media.set(media);
                }
            },
            error: (error) => {
                console.log(error);
                this.error.set(error.message);
            },
            complete: () => {
                this.isLoading.set(false);
            }
     });

     this.destroyRef.onDestroy(() => {
        subscription.unsubscribe();
     })
    }

}
