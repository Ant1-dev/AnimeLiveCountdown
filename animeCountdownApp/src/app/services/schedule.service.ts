import { inject, Injectable } from '@angular/core';
import { Media } from '../models/schedule.model';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, map, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ScheduleService {
  private url: string = environment.apiUrl + 'airing/';
  private httpClient = inject(HttpClient);

  renderMedia(weekDay: string, errorMessage: string) {
    return this.fetchAiring(this.url + weekDay, errorMessage);
  }

  // Weekday tabs: send the viewer's timezone so shows are grouped by the
  // day they air locally rather than in UTC.
  renderWeekDay(weekDay: string, errorMessage: string) {
    const tz = this.viewerTimeZone();
    const params = tz ? new HttpParams().set('tz', tz) : undefined;
    return this.fetchAiring(this.url + weekDay.toUpperCase(), errorMessage, params);
  }

  private viewerTimeZone(): string | undefined {
    try {
      return Intl.DateTimeFormat().resolvedOptions().timeZone || undefined;
    } catch {
      return undefined;
    }
  }

  fetchAiring(url: string, errorMessage: string, params?: HttpParams) {
    console.log('entities are being fetched');
    return this.httpClient.get<Media[]>(url, { params }).pipe(
      map((res) => res),
      catchError((error) => {
        console.log(error);
        return throwError(() => new Error(errorMessage));
      })
    );
  }
}
