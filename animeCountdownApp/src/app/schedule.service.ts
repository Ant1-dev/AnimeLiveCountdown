import { inject, Injectable, signal } from '@angular/core';
import { Media } from './schedule.model';
import { HttpClient } from '@angular/common/http';
import { catchError, map, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ScheduleService {
  private airingMedia = signal<Media[]>([]);
  private httpClient = inject(HttpClient);

  fetchWeekAiring(url: string, errorMessage: string) {
    return this.httpClient.get<Media[]>(url).pipe(
      map((res) => res),
      catchError((error) => {
        console.log(error);
        return throwError(() => new Error(errorMessage));
      })
    );
  }
}
