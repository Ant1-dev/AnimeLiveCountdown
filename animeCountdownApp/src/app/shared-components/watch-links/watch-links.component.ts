import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { MyServicesService } from '../../services/my-services.service';

interface Item {
  label: string;
  url: string;
  mine: boolean;
  own: boolean;
}

/** Most links a show card lists that aren't the viewer's (theirs are always listed). */
const MAX_OTHER_LINKS = 3;

/**
 * "Watch" links for a show card: the viewer's services first and highlighted, a few other services,
 * then the viewer's own platforms (marked as theirs). Renders nothing without links.
 */
@Component({
  selector: 'app-watch-links',
  template: `
    @if (items().length) {
      <div class="watch-row">
        <span class="label">Watch:</span>
        @for (item of items(); track item.label) {
          <a
            [href]="item.url"
            target="_blank"
            [attr.rel]="item.own ? 'noopener noreferrer nofollow' : 'noopener noreferrer'"
            [class.mine]="item.mine"
            [class.own]="item.own"
            [attr.aria-label]="'Watch on ' + item.label + (item.mine ? ', one of your services' : '') + (item.own ? ', your own platform' : '') + ', opens in a new tab'"
            (click)="$event.stopPropagation()"
          >
            @if (item.own) {
              <svg viewBox="0 0 24 24" width="12" height="12" aria-hidden="true">
                <path d="M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
              </svg>
            } @else if (item.mine) {
              <svg viewBox="0 0 24 24" width="12" height="12" aria-hidden="true">
                <path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            }
            {{ item.label }}
          </a>
        }
      </div>
    }
  `,
  styles: `
    .watch-row { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 6px; max-width: 260px; }
    .label { margin-right: 2px; }
    a {
      display: inline-flex; align-items: center; gap: 3px;
      padding: 1px 8px; border-radius: 999px;
      border: 1px solid currentColor; color: inherit; text-decoration: none; font-size: 0.85em; opacity: 0.85;
    }
    a:hover, a:focus-visible { opacity: 1; text-decoration: underline; }
    a.mine { background: var(--accent); border-color: var(--accent); color: var(--on-accent); opacity: 1; font-weight: 600; }
    a.own { border-style: dashed; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WatchLinksComponent {
  private store = inject(MyServicesService);

  mediaId = input<number | undefined>();
  title = input<string | undefined>();

  protected items = computed<Item[]>(() => {
    const links = (this.store.links(this.mediaId()) ?? []).map((l) => ({
      label: this.store.service(l.key)?.label ?? l.key,
      url: l.url,
      mine: this.store.selected().has(l.key),
      own: false,
    }));
    const mine = links.filter((l) => l.mine);
    const others = links.filter((l) => !l.mine).slice(0, MAX_OTHER_LINKS);
    const own = this.store.selection().customPlatforms.map((c) => ({
      label: c.name,
      url: this.store.customUrl(c, this.title()),
      mine: false,
      own: true,
    }));
    return [...mine, ...others, ...own];
  });
}
