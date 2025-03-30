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
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-hero-carousel',
  imports: [],
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

  // In your component
ngAfterViewInit() {
  if (this.mediaList()!.length > 0) {
    const firstSlide = this.mediaList()![0];
    const imgUrl = firstSlide.banner || firstSlide.coverimage;
    
    // Create preload link dynamically
    const link = document.createElement('link');
    link.rel = 'preload';
    link.as = 'image';
    link.href = imgUrl;
    link.type = imgUrl.endsWith('.webp') ? 'image/webp' : 'image/jpeg';
    link.fetchPriority = 'high';
    
    document.head.appendChild(link);
  }
}

  getSafeUrl(url: string | undefined): SafeUrl {
    if (!url) return '';
    return this.sanitizer.bypassSecurityTrustUrl(url);
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

  getOptimizedImageUrl(imageUrl: string): SafeUrl {
    if (!imageUrl) return '';
    
    // Check if the URL already has a format specified
    const hasFormat = /\.(jpe?g|png|gif|webp)$/i.test(imageUrl);
    
    // If it's an existing format, try to serve WebP version if available
    if (hasFormat) {
      // Replace extension with WebP
      const webpUrl = imageUrl.replace(/\.(jpe?g|png|gif)$/i, '.webp');
      return this.sanitizer.bypassSecurityTrustUrl(webpUrl);
    }
    
    // If no format or already WebP, return as is
    return this.sanitizer.bypassSecurityTrustUrl(imageUrl);
  }
}
