import {
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  input,
  output,
  signal,
  OnInit,
  AfterViewInit
} from '@angular/core';
import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MediaInfo } from '../../../../models/media-info.model';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { MediaInfoService } from '../../../../services/media.info.service';

@Component({
  selector: 'app-hero-carousel',
  imports: [],
  templateUrl: './hero-carousel.component.html',
  styleUrl: './hero-carousel.component.css',
})
export class HeroCarouselComponent implements OnInit, AfterViewInit {
  mediaList = input<MediaInfo[]>();
  currentIndex = signal<number>(0);
  autoPlay = signal<boolean>(true);
  autoPlayInterval = signal<number>(7000);
  isAnimating = signal<boolean>(false);
  imagesLoaded = signal<boolean>(false);
  
  currentSlide = computed(() => this.mediaList()![this.currentIndex()]);
  
  private destroyRef = inject(DestroyRef);
  private mediaService = inject(MediaInfoService);
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
  
  ngOnInit() {
    // Preload first slide image
    if (this.mediaList()!.length > 0) {
      this.mediaService.preloadFirstBannerImage(this.mediaList());
    }
  }
  
  ngAfterViewInit() {
    // Track when the first image is loaded
    const firstImage = document.querySelector('.hero-slide:first-child img');
    if (firstImage) {
      if ((firstImage as HTMLImageElement).complete) {
        this.imagesLoaded.set(true);
      } else {
        firstImage.addEventListener('load', () => {
          this.imagesLoaded.set(true);
        });
      }
    }
  }
  
  getSafeUrl(url: string | undefined): SafeUrl {
    if (!url) return '';
    return this.sanitizer.bypassSecurityTrustUrl(url);
  }
  
  getOptimizedImageUrl(url: string | undefined): SafeUrl {
    if (!url) return '';
    const optimizedUrl = this.mediaService.getOptimizedImageUrl(url);
    return this.sanitizer.bypassSecurityTrustUrl(optimizedUrl);
  }
  
  getSrcSet(slide: MediaInfo, isBanner: boolean = true): string {
    return isBanner && slide.banner 
      ? slide.banner 
      : slide.coverimage || '';
  }
  
  getSizes(): string {
    return this.mediaService.getSizesAttribute();
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