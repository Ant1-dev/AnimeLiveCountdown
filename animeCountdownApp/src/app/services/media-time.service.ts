// media-time.service.ts (updated to handle MediaInfo)
import { Injectable, signal, DestroyRef, inject } from '@angular/core';
import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Media } from '../models/schedule.model';
import { MediaInfo } from '../models/media-info.model';
import { TimeRemaining } from '../models/time-remaining.model';

interface TimerData {
  airingTime: number;
  timeLeft: number | undefined;
}

@Injectable({
  providedIn: 'root',
})
export class MediaTimeService {
  // Use a signal for the entire Map to ensure reactivity
  private timerMap = signal<Map<number, TimerData>>(new Map());
  
  private destroyRef = inject(DestroyRef);
  
  constructor() {
    // Set up a single interval for all timers
    interval(1000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.updateAllTimers());
  }
  
  // Get time remaining for a specific media item
  getTimeRemaining(mediaId: number | undefined): TimeRemaining | null {
    if (!mediaId) return null;
    
    const timer = this.timerMap().get(mediaId);
    if (!timer || !timer.timeLeft || timer.timeLeft <= 0) return null;
    
    return {
      days: Math.floor(timer.timeLeft / (1000 * 60 * 60 * 24)),
      hours: Math.floor(
        (timer.timeLeft % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
      ),
      minutes: Math.floor((timer.timeLeft % (1000 * 60 * 60)) / (1000 * 60)),
      seconds: Math.floor((timer.timeLeft % (1000 * 60)) / 1000),
    };
  }
  
  // Helper methods
  getDisplayTitle(media: Media | undefined): string {
    return media?.title_English || media?.title_Romaji || '';
  }
  
  getWebpUrl(url: string | undefined): string {
    if (!url) return '';
    return url.replace(/\.(jpg|jpeg|png)$/i, '.webp');
  }
  
  // Add support for MediaInfo model
  initializeTimerFromMediaInfo(mediaInfo: MediaInfo | undefined): void {
    if (typeof window === 'undefined' || !mediaInfo || !mediaInfo.airingat || !mediaInfo.id) {
      return;
    }
    
    try {
      const airingDate = new Date(mediaInfo.airingat);
      const airingTime = airingDate.getTime();
      
      // Create a new Map with the updated timer
      const updatedMap = new Map(this.timerMap());
      updatedMap.set(mediaInfo.id, {
        airingTime,
        timeLeft: airingTime - Date.now()
      });
      
      // Update the signal with the new Map
      this.timerMap.set(updatedMap);
    } catch (error) {
      console.error('Error processing date:', error);
    }
  }
  
  initializeTimer(media: Media | undefined): void {
    if (typeof window === 'undefined' || !media || !media.next_Airing_At || !media.id) {
      return;
    }
    
    try {
      const airingDate = new Date(media.next_Airing_At);
      const airingTime = airingDate.getTime();
      
      // Create a new Map with the updated timer
      const updatedMap = new Map(this.timerMap());
      updatedMap.set(media.id, {
        airingTime,
        timeLeft: airingTime - Date.now()
      });
      
      // Update the signal with the new Map
      this.timerMap.set(updatedMap);
    } catch (error) {
      console.error('Error processing date:', error);
    }
  }
  
  private updateAllTimers(): void {
    const currentTime = Date.now();
    const currentMap = this.timerMap();
    const updatedMap = new Map<number, TimerData>();
    let hasUpdates = false;
    
    // Update each timer
    currentMap.forEach((timer, mediaId) => {
      const remainingTime = timer.airingTime - currentTime;
      
      if (remainingTime > 0) {
        updatedMap.set(mediaId, {
          airingTime: timer.airingTime,
          timeLeft: remainingTime
        });
        hasUpdates = true;
      } else {
        // Keep expired timers with 0 time
        updatedMap.set(mediaId, {
          airingTime: timer.airingTime,
          timeLeft: 0
        });
      }
    });
    
    // Only update the signal if there were changes
    if (hasUpdates) {
      this.timerMap.set(updatedMap);
    }
  }
}