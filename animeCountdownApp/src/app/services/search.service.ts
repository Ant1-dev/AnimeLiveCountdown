import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { HttpClient } from '@angular/common/http';
import { MediaInfo } from '../models/media-info.model';

@Injectable({
  providedIn: 'root',
})
export class SearchService {
  private url = 'https://anime-countdown-latest.onrender.com/api/search';
  private httpClient = inject(HttpClient);

  search(term: string) {
    if (!term || term == '') {
      return of([]);
    }

    const params = new HttpParams().set('term', term.trim());
    return this.httpClient
      .get<MediaInfo[]>(this.url, { params })
      .pipe(catchError(this.handleError<MediaInfo[]>('search', [])));
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed: ${error.message}`);
      return of(result as T);
    };
  }
}
