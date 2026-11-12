import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationSettings, Reminder, ReminderRequest } from '../../models/notification.model';

/** Reminder preferences and channel settings (Discord webhook, calendar feed). */
@Injectable({ providedIn: 'root' })
export class ReminderService {
  private http = inject(HttpClient);
  private url = environment.apiUrl + 'auth/';

  list(): Observable<Reminder[]> {
    return this.http.get<Reminder[]>(this.url + 'reminders');
  }

  /** The user's reminder for a show, or null if there is none. */
  get(mediaId: number): Observable<Reminder | null> {
    return this.http.get<Reminder>(this.url + `reminders/${mediaId}`).pipe(
      catchError((error: HttpErrorResponse) => (error.status === 404 ? of(null) : throwError(() => error)))
    );
  }

  save(mediaId: number, request: ReminderRequest): Observable<Reminder> {
    return this.http.put<Reminder>(this.url + `reminders/${mediaId}`, request);
  }

  remove(mediaId: number): Observable<void> {
    return this.http.delete<void>(this.url + `reminders/${mediaId}`);
  }

  settings(): Observable<NotificationSettings> {
    return this.http.get<NotificationSettings>(this.url + 'notification-settings');
  }

  setDiscordWebhook(webhookUrl: string): Observable<{ discordWebhookSet: boolean; discordWebhookHint: string }> {
    return this.http.put<{ discordWebhookSet: boolean; discordWebhookHint: string }>(
      this.url + 'notification-settings/discord',
      { webhookUrl }
    );
  }

  removeDiscordWebhook(): Observable<void> {
    return this.http.delete<void>(this.url + 'notification-settings/discord');
  }

  testDiscord(): Observable<{ sent: boolean }> {
    return this.http.post<{ sent: boolean }>(this.url + 'notification-settings/discord/test', null);
  }

  regenerateCalendar(): Observable<{ calendarUrl: string; calendarWebcalUrl: string }> {
    return this.http.post<{ calendarUrl: string; calendarWebcalUrl: string }>(this.url + 'calendar/token', null);
  }

  disableCalendar(): Observable<void> {
    return this.http.delete<void>(this.url + 'calendar/token');
  }
}
