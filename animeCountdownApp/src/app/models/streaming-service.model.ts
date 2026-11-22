/** A streaming service a viewer can pick (GET /api/streaming-services). */
export interface StreamingService {
  key: string;
  label: string;
  color: string;
  /** AniList site names and AnimeSchedule platform keys that mean this service. */
  aliases: string[];
}

/** A platform the viewer added themselves. `url` may contain {title} (the show's URL-encoded title). */
export interface CustomPlatform {
  name: string;
  url: string;
}

/** GET/PUT /api/auth/my-services, and what signed-out visitors keep in localStorage. */
export interface MyServicesSelection {
  services: string[];
  customPlatforms: CustomPlatform[];
}

/** Where a show streams on one service (GET /api/streaming-services/availability). */
export interface ServiceLink {
  key: string;
  url: string;
}

/** For one show: on the viewer's services, only elsewhere, or no streaming data at all. */
export type ServiceStatus = 'mine' | 'other' | 'unknown' | 'loading';
