import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AniListImportResult, AniListStatus } from '../models/follow.model';
import { FollowingService } from './following.service';

/** AniList connect / import for the signed-in user. */
@Injectable({ providedIn: 'root' })
export class AniListService {
  private http = inject(HttpClient);
  private following = inject(FollowingService);
  private readonly url = environment.apiUrl + 'auth/anilist';

  status(): Observable<AniListStatus> {
    return this.http.get<AniListStatus>(this.url);
  }

  /**
   * Starts the OAuth flow and leaves the page for AniList. withCredentials lets the API set the
   * flow cookie it checks when AniList redirects back.
   */
  connect(): Observable<{ authorizeUrl: string }> {
    return this.http
      .post<{ authorizeUrl: string }>(`${this.url}/connect`, {}, { withCredentials: true })
      .pipe(tap(({ authorizeUrl }) => window.location.assign(authorizeUrl)));
  }

  importList(): Observable<AniListImportResult> {
    return this.http
      .post<AniListImportResult>(`${this.url}/import`, {})
      .pipe(tap(() => this.following.load().subscribe({ error: () => {} })));
  }

  disconnect(): Observable<void> {
    return this.http.delete<void>(this.url);
  }
}
