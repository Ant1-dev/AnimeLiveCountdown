export interface MediaInfo {
    id: number;
    engTitle: string;
    romTitle: string;
    nextEpisode: number;
    totalEpisodes: number;
    status: string;
    popularity: number;
    duration: number;
    genres: string[]; 
    avgScore: number;
    description: string;
    coverImage: string;
    bannerImage: string;
    airingAt: string;  
  }