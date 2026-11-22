import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { ReleasesView } from '../models/release.model';

/** Sub and dub release times for a show (GET /api/releases/:mediaId, from AnimeSchedule.net). */
@Injectable({ providedIn: 'root' })
export class ReleaseService {
  private http = inject(HttpClient);

  /** The show's releases, or null if they couldn't be loaded. */
  get(mediaId: number): Observable<ReleasesView | null> {
    return this.http.get<ReleasesView>(`${environment.apiUrl}releases/${mediaId}`).pipe(catchError(() => of(null)));
  }
}
