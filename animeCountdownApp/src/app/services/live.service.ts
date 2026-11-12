import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of, switchMap, timer } from 'rxjs';
import { environment } from '../../environments/environment';
import { LiveResponse } from '../models/live.model';
import { injectIsBrowser, pollEvery } from '../shared/platform';

@Injectable({ providedIn: 'root' })
export class LiveService {
  private http = inject(HttpClient);
  private url = environment.apiUrl + 'live';
  private isBrowser = injectIsBrowser();

  /** Episodes airing around now, refreshed every 2 minutes (the backend caches for 2 minutes too). */
  poll(): Observable<LiveResponse | null> {
    return pollEvery(120_000, this.isBrowser).pipe(
      switchMap(() => this.http.get<LiveResponse>(this.url).pipe(catchError(() => of(null)))),
    );
  }
}
