import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { HypeEntry, ReactionSummary, ReactionType } from '../models/reaction.model';

@Injectable({ providedIn: 'root' })
export class ReactionService {
  private http = inject(HttpClient);

  /** Public counts for the latest aired episode (includes your reaction when signed in). */
  latest(mediaId: number): Observable<ReactionSummary> {
    return this.http.get<ReactionSummary>(`${environment.apiUrl}reactions/${mediaId}`);
  }

  react(mediaId: number, episode: number, reaction: ReactionType): Observable<ReactionSummary> {
    return this.http.put<ReactionSummary>(`${environment.apiUrl}auth/reactions/${mediaId}/${episode}`, { reaction });
  }

  remove(mediaId: number, episode: number): Observable<ReactionSummary> {
    return this.http.delete<ReactionSummary>(`${environment.apiUrl}auth/reactions/${mediaId}/${episode}`);
  }

  mostHyped(limit = 10): Observable<HypeEntry[]> {
    return this.http.get<HypeEntry[]>(`${environment.apiUrl}hype`, { params: { limit } });
  }
}
