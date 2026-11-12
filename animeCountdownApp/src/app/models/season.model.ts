export type SeasonName = 'WINTER' | 'SPRING' | 'SUMMER' | 'FALL';
export type SeasonSort = 'popularity' | 'score' | 'next_airing';

export const SEASON_NAMES: SeasonName[] = ['WINTER', 'SPRING', 'SUMMER', 'FALL'];

export interface SeasonKey {
  season: SeasonName;
  year: number;
}

export interface SeasonMedia {
  id: number;
  engtitle?: string;
  romtitle?: string;
  coverimage?: string;
  genres: string[];
  avgscore?: number;
  popularity?: number;
  totalepisodes?: number;
  status?: string;
  format?: string;
  nextepisode?: number;
  airingat?: string;
}

export interface SeasonResponse {
  season: SeasonName;
  year: number;
  startsAt: string;
  /** 'local' = from our media_info table, 'anilist' = past season fetched from AniList */
  source: 'local' | 'anilist';
  genres: string[];
  media: SeasonMedia[];
}

export interface SeasonOverview {
  current: SeasonKey;
  next: SeasonKey;
  nextStartsAt: string;
  minYear: number;
}
