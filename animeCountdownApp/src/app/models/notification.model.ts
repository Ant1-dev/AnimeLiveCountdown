/** An entry in the notification bell (GET /api/auth/notifications). */
export interface AppNotification {
  id: number;
  kind: string;
  title: string;
  message: string;
  mediaId: number | null;
  episode: number | null;
  imageUrl: string | null;
  airingAt: string | null;
  createdAt: string;
  read: boolean;
  /** Streaming platform URL for sub/dub reminders, when known. */
  link?: string | null;
}

/** Bell entries about a delay or break (DELAY, DELAY_DUB); airingAt is the new date. */
export function isDelayKind(kind: string | null | undefined): boolean {
  return !!kind && kind.startsWith('DELAY');
}

export interface NotificationList {
  items: AppNotification[];
  unread: number;
}

/** Which channels this server has configured (GET /api/notifications/config). */
export interface NotificationConfig {
  pushEnabled: boolean;
  vapidPublicKey: string | null;
  discordEnabled: boolean;
}

/** Which release a reminder follows: the Japanese broadcast, the subbed stream or the English dub. */
export type ReleaseType = 'JAPAN' | 'SUB' | 'DUB';

export const RELEASE_TYPE_OPTIONS: { type: ReleaseType; label: string; hint: string }[] = [
  { type: 'JAPAN', label: 'Japan broadcast', hint: 'Original TV airing' },
  { type: 'SUB', label: 'Subtitled', hint: 'Streaming with English subtitles' },
  { type: 'DUB', label: 'English dub', hint: 'Dubbed release' },
];

export function releaseTypeLabel(type: ReleaseType | null | undefined): string {
  return RELEASE_TYPE_OPTIONS.find((o) => o.type === (type ?? 'JAPAN'))?.label ?? 'Japan broadcast';
}

export interface Reminder {
  mediaId: number;
  leadMinutes: number;
  push: boolean;
  discord: boolean;
  following: boolean;
  title: string | null;
  coverImageUrl: string | null;
  /** For the reminder's release type: the Japanese airing or the next sub/dub release. */
  nextAiringAt: string | null;
  nextEpisode: number | null;
  releaseType: ReleaseType;
  /** Release types this show has times for (JAPAN always). */
  availableTypes: ReleaseType[];
}

export interface ReminderRequest {
  leadMinutes: number;
  push: boolean;
  discord: boolean;
  releaseType?: ReleaseType;
}

export interface NotificationSettings {
  discordEnabled: boolean;
  discordWebhookSet: boolean;
  discordWebhookHint: string | null;
  calendarUrl: string | null;
  calendarWebcalUrl: string | null;
}

export const LEAD_TIME_OPTIONS: { minutes: number; label: string }[] = [
  { minutes: 0, label: 'When it airs' },
  { minutes: 5, label: '5 minutes before' },
  { minutes: 15, label: '15 minutes before' },
  { minutes: 30, label: '30 minutes before' },
  { minutes: 60, label: '1 hour before' },
  { minutes: 120, label: '2 hours before' },
  { minutes: 1440, label: '1 day before' },
];
