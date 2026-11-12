export interface LiveEpisode {
  mediaId: number;
  engtitle?: string;
  romtitle?: string;
  coverimage?: string;
  episode?: number;
  airingAt: string;
  durationMinutes: number;
}

export interface LiveResponse {
  generatedAt: string;
  from: string;
  to: string;
  episodes: LiveEpisode[];
}
