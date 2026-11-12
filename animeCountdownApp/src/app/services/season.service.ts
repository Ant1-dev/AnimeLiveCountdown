import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { SeasonKey, SeasonName, SeasonOverview, SeasonResponse, SeasonSort, SEASON_NAMES } from '../models/season.model';

@Injectable({ providedIn: 'root' })
export class SeasonService {
  private http = inject(HttpClient);
  private url = environment.apiUrl + 'search/season';

  overview(): Observable<SeasonOverview> {
    return this.http.get<SeasonOverview>(this.url + '/overview');
  }

  season(key: SeasonKey, genre: string | null, sort: SeasonSort): Observable<SeasonResponse> {
    let params = new HttpParams().set('season', key.season).set('year', key.year).set('sort', sort);
    if (genre) params = params.set('genre', genre);
    return this.http.get<SeasonResponse>(this.url, { params });
  }
}

/** Same rule as the backend: Jan-Mar WINTER, Apr-Jun SPRING, Jul-Sep SUMMER, Oct-Dec FALL. */
export function seasonOf(date: Date): SeasonKey {
  return { season: SEASON_NAMES[Math.floor(date.getUTCMonth() / 3)], year: date.getUTCFullYear() };
}

export function shiftSeason(key: SeasonKey, by: number): SeasonKey {
  const index = key.year * 4 + SEASON_NAMES.indexOf(key.season) + by;
  return { season: SEASON_NAMES[((index % 4) + 4) % 4], year: Math.floor(index / 4) };
}

export function sameSeason(a: SeasonKey | null | undefined, b: SeasonKey | null | undefined): boolean {
  return !!a && !!b && a.season === b.season && a.year === b.year;
}

export function seasonLabel(season: SeasonName): string {
  return season.charAt(0) + season.slice(1).toLowerCase();
}

/** "in 12 days", "in 5h 20m", "in 3m", or "now" for times in the past. */
export function timeUntil(target: number, now: number): string {
  const ms = target - now;
  if (ms <= 0) return 'now';
  const minutes = Math.floor(ms / 60000);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);
  if (days >= 2) return `in ${days} days`;
  if (days === 1) return `in 1 day ${hours % 24}h`;
  if (hours >= 1) return `in ${hours}h ${minutes % 60}m`;
  return `in ${Math.max(minutes, 1)}m`;
}
