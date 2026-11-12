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

export interface Reminder {
  mediaId: number;
  leadMinutes: number;
  push: boolean;
  discord: boolean;
  following: boolean;
  title: string | null;
  coverImageUrl: string | null;
  nextAiringAt: string | null;
  nextEpisode: number | null;
}

export interface ReminderRequest {
  leadMinutes: number;
  push: boolean;
  discord: boolean;
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
