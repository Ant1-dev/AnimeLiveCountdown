import { Injectable, signal, computed, DestroyRef, inject } from '@angular/core';
import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Media } from '../models/schedule.model';
import { MediaTimeService } from './media-time.service';

interface MediaStore {
  [weekDay: string]: Media[];
}

@Injectable({
  providedIn: 'root',
})
export class MediaStoreService {
  private destroyRef = inject(DestroyRef);
  private mediaTimeService = inject(MediaTimeService);

  // Store for each day of the week (up to 15 items per day)
  private mediaStore = signal<MediaStore>({});

  // Number of items to display at once
  private readonly DISPLAY_COUNT = 6;

  // Maximum items to keep in store
  private readonly MAX_STORE_SIZE = 15;

  constructor() {
    // Check every 30 seconds if we need to pull from the queue
    interval(30000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.checkAndUpdateDisplayedMedia());
  }

  /**
   * Add media to the store for a specific weekday
   * Maintains priority queue sorted by airing time
   */
  addMediaToStore(weekDay: string, newMedia: Media[]): void {
    const currentStore = this.mediaStore();
    const existingMedia = currentStore[weekDay] || [];

    // Combine existing and new media
    const combined = [...existingMedia];

    // Add new media that doesn't already exist
    newMedia.forEach(media => {
      const exists = combined.some(m => m.id === media.id);
      if (!exists) {
        combined.push(media);
      }
    });

    // Sort by airing time (earliest first) - priority queue behavior
    const sorted = combined.sort((a, b) => {
      const timeA = new Date(a.next_Airing_At).getTime();
      const timeB = new Date(b.next_Airing_At).getTime();
      return timeA - timeB;
    });

    // Keep only the first MAX_STORE_SIZE items
    const trimmed = sorted.slice(0, this.MAX_STORE_SIZE);

    // Update the store
    this.mediaStore.set({
      ...currentStore,
      [weekDay]: trimmed,
    });
  }

  /**
   * Get media to display for a specific weekday
   * Returns only DISPLAY_COUNT items that haven't aired yet
   */
  getDisplayMedia(weekDay: string): Media[] {
    const store = this.mediaStore()[weekDay] || [];

    // Filter out aired media and get only DISPLAY_COUNT items
    const activeMedia = store.filter(media => {
      const timeRemaining = this.mediaTimeService.getTimeRemaining(media.id);
      return timeRemaining !== null;
    });

    return activeMedia.slice(0, this.DISPLAY_COUNT);
  }

  /**
   * Get computed signal for display media
   */
  getDisplayMediaSignal(weekDay: string) {
    return computed(() => this.getDisplayMedia(weekDay));
  }

  /**
   * Get the full store for a weekday (for debugging or admin purposes)
   */
  getFullStore(weekDay: string): Media[] {
    return this.mediaStore()[weekDay] || [];
  }

  /**
   * Remove aired media from the store
   */
  private removeAiredMedia(weekDay: string): void {
    const currentStore = this.mediaStore();
    const media = currentStore[weekDay] || [];

    const activeMedia = media.filter(m => {
      const timeRemaining = this.mediaTimeService.getTimeRemaining(m.id);
      return timeRemaining !== null;
    });

    if (activeMedia.length !== media.length) {
      this.mediaStore.set({
        ...currentStore,
        [weekDay]: activeMedia,
      });
    }
  }

  /**
   * Check all stores and remove aired media
   */
  private checkAndUpdateDisplayedMedia(): void {
    const currentStore = this.mediaStore();
    const weekDays = Object.keys(currentStore);

    weekDays.forEach(weekDay => {
      this.removeAiredMedia(weekDay);
    });
  }

  /**
   * Clear store for a specific weekday
   */
  clearStore(weekDay: string): void {
    const currentStore = this.mediaStore();
    const { [weekDay]: _, ...rest } = currentStore;
    this.mediaStore.set(rest);
  }

  /**
   * Get count of active (non-aired) media in store
   */
  getActiveCount(weekDay: string): number {
    const store = this.mediaStore()[weekDay] || [];
    return store.filter(media => {
      const timeRemaining = this.mediaTimeService.getTimeRemaining(media.id);
      return timeRemaining !== null;
    }).length;
  }
}
