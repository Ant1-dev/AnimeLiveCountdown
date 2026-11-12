import { formatDate } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, LOCALE_ID } from '@angular/core';
import { ScheduleNote } from '../../models/schedule-note.model';

const HOUR = 3_600_000;
const DAY = 24 * HOUR;
const WEEK = 7 * DAY;

// "On break · returns Nov 20" / "Delayed 1 week". Renders nothing when there's no note.
@Component({
  selector: 'app-schedule-note-badge',
  template: `
    @if (label(); as text) {
      <span class="badge" [class.delayed]="note()?.type === 'DELAYED'" [attr.title]="text">{{ text }}</span>
    }
  `,
  styleUrl: './schedule-note-badge.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.overlay]': "variant() === 'overlay'" },
})
export class ScheduleNoteBadgeComponent {
  note = input<ScheduleNote | null | undefined>();
  // 'overlay' pins it to the bottom-left of a position: relative parent (e.g. a poster).
  variant = input<'inline' | 'overlay'>('inline');
  private locale = inject(LOCALE_ID);

  label = computed(() => {
    const note = this.note();
    if (!note) return null;
    const expiresAt = new Date(note.expiresAt);
    if (note.type === 'ON_BREAK') {
      return `On break · returns ${formatDate(expiresAt, 'MMM d', this.locale)}`;
    }
    const delay = expiresAt.getTime() - new Date(note.originalAt).getTime();
    return `Delayed ${this.describeDelay(delay, expiresAt)}`;
  });

  private describeDelay(ms: number, newDate: Date): string {
    if (ms >= 4.5 * WEEK) return `to ${formatDate(newDate, 'MMM d', this.locale)}`;
    const weeks = Math.round(ms / WEEK);
    if (weeks >= 1 && Math.abs(ms - weeks * WEEK) <= DAY) return plural(weeks, 'week');
    if (ms >= DAY) return plural(Math.round(ms / DAY), 'day');
    return plural(Math.max(1, Math.round(ms / HOUR)), 'hour');
  }
}

function plural(n: number, unit: string): string {
  return `${n} ${unit}${n === 1 ? '' : 's'}`;
}
