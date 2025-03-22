import { inject, Injectable, signal } from '@angular/core';
import { Media } from '../schedule.model';
import { catchError, Observable, of } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { HttpClient } from '@angular/common/http';

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
      .get<Media[]>(this.url, { params })
      .pipe(catchError(this.handleError<Media[]>('search', [])));
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed: ${error.message}`);
      return of(result as T);
    };
  }
}
