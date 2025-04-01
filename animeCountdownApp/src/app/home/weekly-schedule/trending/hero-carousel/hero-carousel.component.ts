import {
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MediaInfo } from '../../../../models/media-info.model';
import { DomSanitizer } from '@angular/platform-browser';
import { NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-hero-carousel',
  imports: [NgOptimizedImage],
  templateUrl: './hero-carousel.component.html',
  styleUrl: './hero-carousel.component.css',
})
export class HeroCarouselComponent {
  mediaList = input<MediaInfo[]>();
  currentIndex = signal<number>(0);
  autoPlay = signal<boolean>(true);
  autoPlayInterval = signal<number>(7000);
  isAnimating = signal<boolean>(false);
  currentSlide = computed(() => this.mediaList()![this.currentIndex()]);
  private destroyRef = inject(DestroyRef);
  index = output<number>();
  sanitizer = inject(DomSanitizer);

  constructor() {
    effect(() => {
      if (this.autoPlay()) {
        this.startAutoPlay();
      }
      this.index.emit(this.currentIndex());
    });
  }

  startAutoPlay() {
    interval(this.autoPlayInterval())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.nextSlide();
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
    const newIndex = (this.currentIndex() + 1) % this.mediaList()!.length;
    this.goToSlide(newIndex);
  }

  prevSlide() {
    const newIndex =
      (this.currentIndex() - 1 + this.mediaList()!.length) %
      this.mediaList()!.length;
    this.goToSlide(newIndex);
  }

  truncateWithEllipsis(text: string | undefined, limit: number): string {
    if (!text) return '';
    if (text.length <= limit) return text;
    return text.slice(0, limit) + '...';
  }
}
