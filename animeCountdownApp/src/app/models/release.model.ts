/** GET /api/releases/:mediaId (times from AnimeSchedule.net). */
export interface ReleasesView {
  /** False when the feature isn't configured on the server; the section is hidden. */
  enabled: boolean;
  /** Keyed by RAW / SUB / DUB; each is the next release, or the latest if none is scheduled. */
  releases: Partial<Record<'RAW' | 'SUB' | 'DUB', Release>>;
}

export interface Release {
  type: 'RAW' | 'SUB' | 'DUB';
  episode: number;
  releaseAt: string;
  streams: { platform: string; label: string; url: string }[];
  delayedUntil?: string | null;
  delayNote?: string | null;
}
