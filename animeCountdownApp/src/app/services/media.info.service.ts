import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { MediaInfo } from '../models/media-info.model';
import { catchError, Observable, throwError, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';

@Injectable({
  providedIn: 'root',
})
export class MediaInfoService {
  private baseUrl = environment.apiUrl + 'info/';
  private httpClient = inject(HttpClient);
  private mediaId = signal<number | undefined>(undefined);
  private sanitizer = inject(DomSanitizer);
  
  // Image format support detection
  private supportsWebP = signal<boolean>(false);
  
  constructor() {
    // Check WebP support on initialization
    this.checkWebPSupport();
  }
  
  // Check if browser supports WebP
  private checkWebPSupport(): void {
    const webP = new Image();
    webP.onload = () => this.supportsWebP.set(true);
    webP.onerror = () => this.supportsWebP.set(false);
    webP.src = 'data:image/webp;base64,UklGRiQAAABXRUJQVlA4IBgAAAAwAQCdASoBAAEAAwA0JaQAA3AA/vuUAAA=';
  }
  
  getMedia() {
    return this.httpClient.get<MediaInfo>(this.baseUrl + this.mediaId()).pipe(
      map(media => this.enhanceMediaWithOptimizedImages(media)),
      catchError((error) => {
        console.log(error);
        return throwError(() => new Error(error));
      })
    );
  }
  
  getTrendingMedia(error: string): Observable<MediaInfo[]> {
    return this.httpClient.get<any>(this.baseUrl + 'trending').pipe(
      map(response => {
        const mediaList = response.media || response;
        const dimensions = response.dimensions || {};
        
        return mediaList.map((media: any) => {
          // Create enhanced media objects with dimension info
          const enhancedMedia = {...media};
          
          // Add dimension properties if available in response
          if (dimensions[media.id]) {
            enhancedMedia.bannerWidth = dimensions[media.id].width;
            enhancedMedia.bannerHeight = dimensions[media.id].height;
          }
          
          // Add optimized image paths
          return this.enhanceMediaWithOptimizedImages(enhancedMedia);
        });
      }),
      catchError((error) => {
        console.log(error);
        return throwError(() => new Error(error));
      })
    );
  }
  
  getMediaId(id: number) {
    this.mediaId.set(id);
  }
  
  private enhanceMediaWithOptimizedImages(media: any): MediaInfo {
    const enhanced = {...media};
    
    // Add optimized banner images if banner exists
    if (enhanced.banner) {
      enhanced.optimizedBanner = this.getOptimizedImageUrl(enhanced.banner);
      enhanced.bannerSrcSet = this.generateSrcSet(enhanced.banner);
    }
    
    // Add optimized cover images
    if (enhanced.coverimage) {
      enhanced.optimizedCover = this.getOptimizedImageUrl(enhanced.coverimage);
      enhanced.coverSrcSet = this.generateSrcSet(enhanced.coverimage);
    }
    
    return enhanced;
  }
  
  // Generate WebP version URL if supported
  getOptimizedImageUrl(imageUrl: string): string {
    if (!imageUrl) return '';
    
    if (!this.supportsWebP()) return imageUrl;
    
    if (imageUrl.toLowerCase().endsWith('.webp')) return imageUrl;
    
    if (imageUrl.includes('anilist.co')) {
      return imageUrl.includes('?') 
        ? `${imageUrl}&format=webp`
        : `${imageUrl}?format=webp`;
    }
    return imageUrl.replace(/\.(jpe?g|png|gif)$/i, '.webp');
  }
  
  // Generate srcset for responsive images
  generateSrcSet(imageUrl: string): string {
    if (!imageUrl) return '';
    
    const baseUrl = imageUrl.split('?')[0]; // Remove query params
    const params = imageUrl.includes('?') ? imageUrl.split('?')[1] : '';
    

    const smallUrl = this.getOptimizedImageUrl(
      params ? `${baseUrl}?width=640&${params}` : `${baseUrl}?width=640`
    );
    
    const mediumUrl = this.getOptimizedImageUrl(
      params ? `${baseUrl}?width=1280&${params}` : `${baseUrl}?width=1280`
    );
    
    const largeUrl = this.getOptimizedImageUrl(imageUrl); // Original size with WebP
    
    return `${smallUrl} 640w, ${mediumUrl} 1280w, ${largeUrl} 1920w`;
  }
  
  // Get sizes attribute for responsive images
  getSizesAttribute(): string {
    return '(max-width: 640px) 640px, (max-width: 1280px) 1280px, 1920px';
  }
  
  getSafeUrl(url: string): SafeUrl {
    if (!url) return '';
    return this.sanitizer.bypassSecurityTrustUrl(url);
  }
  
  preloadFirstBannerImage(mediaList: MediaInfo[] | undefined): void {
    if (!mediaList || mediaList.length === 0) return;
    
    const firstMedia = mediaList[0];
    const imageUrl = firstMedia.banner || firstMedia.coverimage;
    
    if (imageUrl) {
      const link = document.createElement('link');
      link.rel = 'preload';
      link.as = 'image';
      link.href = this.getOptimizedImageUrl(imageUrl);
      link.fetchPriority = 'high';
      document.head.appendChild(link);
      
      // Also preload the image directly for immediate carousel use
      const img = new Image();
      img.src = this.getOptimizedImageUrl(imageUrl);
    }
  }
}