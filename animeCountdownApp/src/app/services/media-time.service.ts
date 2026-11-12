import { Injectable, signal, DestroyRef, inject } from '@angular/core';
import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Media } from '../models/schedule.model';
import { MediaInfo } from '../models/media.info.model';
import { TimeRemaining } from '../models/time-remaining.model';
import { injectIsBrowser, tickEvery } from '../shared/platform';

interface TimerData {
  airingTime: number;
  timeLeft: number | undefined;
  title?: string;
  episode?: number;
}

/** A countdown that reached zero while the page was open. */
export interface AiredEvent {
  mediaId: number;
  title: string;
  episode?: number;
  at: number;
}

@Injectable({
  providedIn: 'root',
})
export class MediaTimeService {
  // Use a signal for the entire Map to ensure reactivity
  private timerMap = signal<Map<number, TimerData>>(new Map());

  private destroyRef = inject(DestroyRef);

  /** Countdowns that just hit zero, newest last. The airing celebration listens to this. */
  readonly justAired = signal<AiredEvent[]>([]);

  constructor() {
    // Set up a single interval for all timers
    tickEvery(1000, injectIsBrowser())
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
    if (
      !mediaInfo ||
      !mediaInfo.airingat
    ) {
      return;
    }

    try {
      const airingDate = new Date(mediaInfo.airingat);
      const airingTime = airingDate.getTime();

      // Create a new Map with the updated timer
      const updatedMap = new Map(this.timerMap());
      updatedMap.set(mediaInfo.id, {
        airingTime,
        timeLeft: airingTime - Date.now(),
        title: mediaInfo.engtitle || mediaInfo.romtitle,
        episode: mediaInfo.nextepisode,
      });

      // Update the signal with the new Map
      this.timerMap.set(updatedMap);
    } catch (error) {
      console.error('Error processing date:', error);
    }
  }

  initializeTimer(media: Media | undefined): void {
    if (
      !media ||
      !media.next_Airing_At ||
      !media.id
    ) {
      return;
    }

    try {
      const airingDate = new Date(media.next_Airing_At);
      const airingTime = airingDate.getTime();

      // Create a new Map with the updated timer
      const updatedMap = new Map(this.timerMap());
      updatedMap.set(media.id, {
        airingTime,
        timeLeft: airingTime - Date.now(),
        title: this.getDisplayTitle(media),
        episode: media.next_Airing_Episode,
      });

      // Update the signal with the new Map
      this.timerMap.set(updatedMap);
    } catch (error) {
      console.error('Error processing date:', error);
    }
  }

  /**
   * Tracks an upcoming episode that may not have a card on screen (e.g. from the live banner) so
   * the celebration can fire for it. Doesn't replace a timer that's already tracking that show.
   */
  watchAiring(mediaId: number, airingAt: string | Date, title: string, episode?: number): void {
    if (typeof window === 'undefined' || this.timerMap().has(mediaId)) return;
    const airingTime = new Date(airingAt).getTime();
    if (!Number.isFinite(airingTime) || airingTime <= Date.now()) return;
    const updatedMap = new Map(this.timerMap());
    updatedMap.set(mediaId, { airingTime, timeLeft: airingTime - Date.now(), title, episode });
    this.timerMap.set(updatedMap);
  }

  dismissAired(mediaId: number): void {
    this.justAired.update((events) => events.filter((e) => e.mediaId !== mediaId));
  }

  private updateAllTimers(): void {
    const currentTime = Date.now();
    const currentMap = this.timerMap();
    const updatedMap = new Map<number, TimerData>();
    const aired: AiredEvent[] = [];
    let hasUpdates = false;

    // Update each timer
    currentMap.forEach((timer, mediaId) => {
      const remainingTime = timer.airingTime - currentTime;

      if (remainingTime > 0) {
        updatedMap.set(mediaId, {
          ...timer,
          timeLeft: remainingTime,
        });
        hasUpdates = true;
      } else {
        // Crossed zero during this tick (not a timer that was already expired when added)
        if (timer.timeLeft && timer.timeLeft > 0) {
          aired.push({ mediaId, title: timer.title || 'A new episode', episode: timer.episode, at: currentTime });
          hasUpdates = true;
        }
        // Keep expired timers with 0 time
        updatedMap.set(mediaId, {
          ...timer,
          timeLeft: 0,
        });
      }
    });

    // Only update the signal if there were changes
    if (hasUpdates) {
      this.timerMap.set(updatedMap);
    }
    if (aired.length) {
      this.justAired.update((events) => [...events, ...aired].slice(-5));
    }
  }
}
