import { inject, Injectable, signal } from '@angular/core';
import { Media } from '../models/schedule.model';
import { HttpClient } from '@angular/common/http';
import { catchError, map, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ScheduleService {
  private url: string = environment.apiUrl + 'airing/';
  private httpClient = inject(HttpClient);

  renderMedia(weekDay: string, errorMessage: string) {
    return this.fetchWeekAiring(this.url + weekDay, errorMessage);
  }

  fetchWeekAiring(url: string, errorMessage: string) {
    console.log('entities are being fetched');
    return this.httpClient.get<Media[]>(url).pipe(
      map((res) => res),
      catchError((error) => {
        console.log(error);
        return throwError(() => new Error(errorMessage));
      })
    );
  }
}
