import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { MediaInfo } from '../models/media.info.model';
import { catchError, Observable, throwError, map } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class MediaInfoService {
  private baseUrl = environment.apiUrl + 'info/';
  private httpClient = inject(HttpClient);
  private mediaId = signal<number | undefined>(undefined);

  getMedia() {
    return this.httpClient.get<MediaInfo>(this.baseUrl + this.mediaId()).pipe(
      catchError((error) => {
        console.log(error);
        return throwError(() => new Error(error));
      })
    );
  }

  getTrendingMedia(error: string): Observable<MediaInfo[]> {
    return this.httpClient.get<any>(this.baseUrl + 'trending').pipe(
      map((response) => {
        // Handle both formats - normal array or object with dimensions
        const mediaList = response.media || response;
        const dimensions = response.dimensions || {};

        return mediaList.map((media: any) => {
          const enhancedMedia = { ...media };

          if (dimensions[media.id]) {
            enhancedMedia.bannerWidth = dimensions[media.id].width;
            enhancedMedia.bannerHeight = dimensions[media.id].height;
          }

          return enhancedMedia;
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
}
