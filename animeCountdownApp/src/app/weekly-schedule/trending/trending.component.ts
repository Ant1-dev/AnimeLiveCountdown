import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ScheduleService } from '../../schedule.service';
import { Media } from '../../schedule.model';
import { ShowComponent } from "../shows/show/show.component";

@Component({
    selector: 'app-trending',
    imports: [CommonModule, ShowComponent],
    templateUrl: './trending.component.html',
    styleUrl: './trending.component.css'
})
export class TrendingComponent {
     media = signal<Media[]>([]);
     isLoading = signal(false);
     error = signal<string>('');
     private scheduleService = inject(ScheduleService);
     private destroyRef = inject(DestroyRef);
   
     ngOnInit(): void {
       this.isLoading.set(true);
   
       const subscription = this.scheduleService
         .renderMedia('trending', this.error())
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
