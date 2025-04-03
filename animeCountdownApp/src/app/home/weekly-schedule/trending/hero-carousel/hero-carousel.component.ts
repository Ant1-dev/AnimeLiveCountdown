import { Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { MediaInfo } from '../../../../models/media.info.model';
import { CommonModule, NgOptimizedImage } from '@angular/common';
import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TimeRemaining } from '../../../../models/time-remaining.model';

@Component({
  selector: 'app-hero-carousel',
  imports: [NgOptimizedImage, CommonModule],
  templateUrl: './hero-carousel.component.html',
  styleUrl: './hero-carousel.component.css',
})
export class HeroCarouselComponent implements OnInit {
  mediaList = input.required<MediaInfo[]>();
  currentIndex = signal<number>(0);
  currentMedia = computed(() => this.mediaList()[this.currentIndex()]);
  isAnimating = signal<boolean>(false);
  destroyRef = inject(DestroyRef);
  timeRemaining = signal<TimeRemaining>({ days: 0, hours: 0, minutes: 0, seconds: 0 });
  
  ngOnInit(): void {
    queueMicrotask(() => {
      interval(10000)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => this.nextSlide());
  
      interval(1000)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => {
          this.timeRemaining.set(this.calculateTimeLeft(this.currentMedia().airingat));
        });
    });
  }

  goToSlide(index: number) {
    if (this.isAnimating()) return;
    this.isAnimating.set(true);
    this.currentIndex.set(index);
    setTimeout(() => {
      this.isAnimating.set(false);
    }, 600);
  }

  nextSlide() {
    const newIndex = (this.currentIndex() + 1) % this.mediaList().length;
    this.goToSlide(newIndex);
  }

  trimWithDots(text: string, limit: number): string {
    if (text.length <= limit) return text;
    return text.slice(0, limit) + '...';
  }

  calculateTimeLeft(targetDate: Date | undefined): TimeRemaining {
    if (!targetDate) {
      return { days: 0, hours: 0, minutes: 0, seconds: 0 };
    }
    
    const validTargetDate = targetDate instanceof Date ? targetDate : new Date(targetDate);
    
    if (isNaN(validTargetDate.getTime()) || validTargetDate < new Date()) {
      return { days: 0, hours: 0, minutes: 0, seconds: 0 };
    }
    
    const differenceMs = validTargetDate.getTime() - new Date().getTime();
    
    return {
      days: Math.floor(differenceMs / (1000 * 60 * 60 * 24)),
      hours: Math.floor((differenceMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)),
      minutes: Math.floor((differenceMs % (1000 * 60 * 60)) / (1000 * 60)),
      seconds: Math.floor((differenceMs % (1000 * 60)) / 1000)
    };
  }
}