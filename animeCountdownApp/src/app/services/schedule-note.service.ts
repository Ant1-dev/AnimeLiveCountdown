import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';
import { environment } from '../../environments/environment';
import { ScheduleNote } from '../models/schedule-note.model';

@Injectable({
  providedIn: 'root',
})
export class ScheduleNoteService {
  private baseUrl = environment.apiUrl + 'airing/note/';
  private httpClient = inject(HttpClient);

  // The backend answers 204 (null body) when the show is on schedule.
  getNote(mediaId: number): Observable<ScheduleNote | null> {
    return this.httpClient.get<ScheduleNote | null>(this.baseUrl + mediaId).pipe(
      map((note) => note ?? null),
      catchError(() => of(null))
    );
  }
}
