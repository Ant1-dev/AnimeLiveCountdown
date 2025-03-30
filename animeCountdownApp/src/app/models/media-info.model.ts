export interface MediaInfo {
  id: number;
  engtitle?: string;
  romtitle?: string;
  nextepisode?: number;
  totalepisodes?: number;
  status?: string;
  popularity?: number;
  duration?: number;
  genres?: string[];
  avgscore?: number;
  description?: string;
  coverimage: string;
  banner?: string;
  airingat?: string;
  
  // additional properties for performance optimization
  bannerWidth?: number;
  bannerHeight?: number;
}