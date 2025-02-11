import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { Media } from '../../../schedule.model';
import { interval } from 'rxjs';

@Component({
  selector: 'app-show',
  imports: [CommonModule],
  templateUrl: './show.component.html',
  styleUrl: './show.component.css',
})
export class ShowComponent implements OnInit {
  media = input<Media>();
  timeLeft = signal<Date | undefined | string>(undefined);
  private airingTime: number | undefined = this.media()?.next_Airing_At.getMilliseconds();
  private destroyRef = inject(DestroyRef);
  
  
  ngOnInit(): void {

    if (typeof window === 'undefined') {
      return;
    }
    const mediaData = this.media();
    
    // Early return if no media data
    if (!mediaData) {
        console.log('No media data available');
        this.timeLeft.set("000000");
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
                    this.timeLeft.set(new Date(remainingTime));
                } else {
                    this.timeLeft.set("0");
                    subscription.unsubscribe();
                }
            } else {
                this.timeLeft.set("0");
                subscription.unsubscribe();
            }
        });

        this.destroyRef.onDestroy(() => {
            subscription.unsubscribe();
        });

    } catch (error) {
        console.error('Error processing date:', error);
        this.timeLeft.set("000000");
    }
}
}
