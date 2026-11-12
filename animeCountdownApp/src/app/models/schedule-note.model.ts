// Set by the backend sync when a show is off its usual schedule. Absent/null when on schedule.
export interface ScheduleNote {
  type: 'ON_BREAK' | 'DELAYED';
  detectedAt: string;
  // ON_BREAK: when the previous episode aired. DELAYED: the originally announced time.
  originalAt: string;
  // The episode's new airing time (the return date for ON_BREAK).
  expiresAt: string;
}
