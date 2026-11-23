import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type IconName =
  | 'flame'
  | 'heart'
  | 'laugh'
  | 'shock'
  | 'sad'
  | 'meh'
  | 'star'
  | 'users'
  | 'sparkles';

/**
 * Inline SVG icons, used instead of emoji so icons look the same on every platform and follow the
 * theme (they draw in currentColor). Decorative by default; pass `label` when the icon stands alone.
 */
@Component({
  selector: 'app-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'app-icon',
    '[attr.role]': 'label() ? "img" : null',
    '[attr.aria-label]': 'label() || null',
    '[attr.aria-hidden]': 'label() ? null : "true"',
  },
  template: `
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      [attr.width]="size()"
      [attr.height]="size()"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      focusable="false"
    >
      @switch (name()) {
        @case ('flame') {
          <path
            d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.07-2.14-.22-4.05 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.15.43-2.29 1-3a2.5 2.5 0 0 0 2.5 2.5z"
          />
        }
        @case ('heart') {
          <path
            d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7z"
          />
        }
        @case ('laugh') {
          <circle cx="12" cy="12" r="10" />
          <path d="M18 13a6 6 0 0 1-12 0z" />
          <path d="M9 9h.01M15 9h.01" />
        }
        @case ('shock') {
          <circle cx="12" cy="12" r="10" />
          <circle cx="12" cy="15.5" r="2" />
          <path d="M9 9.5h.01M15 9.5h.01M7.5 6.5 9.5 7.5M16.5 6.5l-2 1" />
        }
        @case ('sad') {
          <circle cx="12" cy="12" r="10" />
          <path d="M16 16s-1.5-2-4-2-4 2-4 2" />
          <path d="M9 9h.01M15 9h.01" />
        }
        @case ('meh') {
          <circle cx="12" cy="12" r="10" />
          <path d="M8 15h8" />
          <path d="M9 9h.01M15 9h.01" />
        }
        @case ('star') {
          <path
            fill="currentColor"
            d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01z"
          />
        }
        @case ('users') {
          <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
        }
        @case ('sparkles') {
          <path
            d="M9.94 15.5a2 2 0 0 0-1.44-1.44l-6.13-1.58a.5.5 0 0 1 0-.96L8.5 9.94A2 2 0 0 0 9.94 8.5l1.58-6.13a.5.5 0 0 1 .96 0l1.58 6.13a2 2 0 0 0 1.44 1.44l6.13 1.58a.5.5 0 0 1 0 .96l-6.13 1.58a2 2 0 0 0-1.44 1.44l-1.58 6.13a.5.5 0 0 1-.96 0z"
          />
          <path d="M20 3v4M22 5h-4M4 17v2M5 18H3" />
        }
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex-shrink: 0;
      line-height: 0;
      vertical-align: -0.125em;
    }
  `,
})
export class IconComponent {
  name = input.required<IconName>();
  size = input<number | string>(18);
  /** Accessible name; leave empty for decorative icons next to text. */
  label = input<string>('');
}
