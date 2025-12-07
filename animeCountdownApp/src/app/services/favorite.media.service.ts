import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { FavoriteMedia } from '../models/favorite.media.model';
import { catchError, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class FavoriteMediaService {
  private url = environment.apiUrl + 'auth/favorite';
  private httpClient = inject(HttpClient);

  getFavoriteMedia(userId: number) {
    return this.httpClient.get<FavoriteMedia>(this.url + `/${userId}`);
  }

  addFavoriteMedia(userId: number, mediaId: number) {
    return this.httpClient.post(this.url + `/${userId}/${mediaId}`, {}).pipe(
      catchError((error) => {
        console.log(error);
        return throwError(() => new Error(error));
      })
    );
  }
}
