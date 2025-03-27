import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { MediaInfo } from '../models/media-info.model';
import { catchError, map, throwError } from 'rxjs';
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

  getMediaId(id: number) {
    this.mediaId.set(id);
  }
}
