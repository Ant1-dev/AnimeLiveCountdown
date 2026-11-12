export interface MediaInfo {
  id: number;
  engtitle?: string;
  romtitle?: string;
  nextepisode?: number;
  totalepisodes?: number;
  status: string;
  popularity: number;
  duration?: number;
  genres: string[];
  avgscore?: number;
  description: string;
  coverimage: string;
  banner: string;
  airingat?: Date;
  season?: string;
  seasonYear?: number;
  streamingLinks?: StreamingLink[];
}

/** Legal streaming site from AniList externalLinks. */
export interface StreamingLink {
  site: string;
  url: string;
  icon?: string;
  color?: string;
  language?: string;
}