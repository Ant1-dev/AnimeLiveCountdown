import { Media } from './schedule.model';

/** A followed show and the viewer's episode progress (GET /api/auth/following). */
export interface Follow {
  media: Media;
  watchedEpisode: number;
  /** Latest episode that has aired; null when unknown (e.g. a finished show with no episode count). */
  latestAiredEpisode: number | null;
  /** Aired episodes not watched yet; null when latestAiredEpisode is unknown. */
  behind: number | null;
  addedDate: string;
  progressUpdatedAt: string | null;
}

export interface AniListStatus {
  enabled: boolean;
  connected: boolean;
  userName: string | null;
  lastSyncedAt: string | null;
}

export interface AniListImportResult {
  entries: number;
  followed: number;
  progressUpdated: number;
  unchanged: number;
  showsAdded: number;
  skippedNotAiring: number;
  skippedAdult: number;
  syncedAt: string;
}
