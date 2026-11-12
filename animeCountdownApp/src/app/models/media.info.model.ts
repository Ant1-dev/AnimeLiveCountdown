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
  /** AniList trailer: "youtube" or "dailymotion", with that site's video id. */
  trailerSite?: string;
  trailerId?: string;
  trailerThumbnail?: string;
}

/** Legal streaming site from AniList externalLinks. */
export interface StreamingLink {
  site: string;
  url: string;
  icon?: string;
  color?: string;
  language?: string;
}
/** One entry of a franchise's watch order (GET /api/info/:id/watch-order). */
export interface WatchOrderEntry {
  id: number;
  title: string;
  format?: string;
  year?: number;
  episodes?: number;
  status?: string;
  coverImage?: string;
  /** MAIN: the prequel/sequel line. SIDE: movies, OVAs, specials. */
  kind: 'MAIN' | 'SIDE';
  current: boolean;
  /** This site has a page for it; otherwise link to AniList. */
  onSite: boolean;
}
