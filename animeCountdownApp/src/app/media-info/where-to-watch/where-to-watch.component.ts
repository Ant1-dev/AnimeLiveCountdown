import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StreamingLink } from '../../models/media.info.model';
import { MyServicesService } from '../../services/my-services.service';

interface WatchLink extends StreamingLink {
  mine: boolean;
}

/**
 * Streaming buttons from AniList's externalLinks (type STREAMING), the viewer's services first and
 * highlighted, then the viewer's own platforms. Renders nothing when there's nothing to show.
 */
@Component({
  selector: 'app-where-to-watch',
  imports: [RouterLink],
  template: `
    @if (sorted().length || customs().length) {
      <div class="watch" id="where-to-watch">
        <div class="head">
          <h4>Where to watch</h4>
          <a class="edit" routerLink="/my-services">
            <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
              <rect x="3" y="5" width="18" height="12" rx="2" fill="none" stroke="currentColor" stroke-width="2" />
              <path d="M8 20h8" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
            </svg>
            {{ store.hasAny() ? 'Edit my services' : 'Set your services' }}
          </a>
        </div>
        <ul>
          @for (link of sorted(); track link.url) {
            <li>
              <a
                class="watch-btn"
                [class.mine]="link.mine"
                [href]="link.url"
                target="_blank"
                rel="noopener noreferrer"
                [style.--site-color]="link.color || null"
                [attr.aria-label]="'Watch on ' + link.site + (link.language ? ' (' + link.language + ')' : '') + (link.mine ? ', one of your services' : '') + ', opens in a new tab'"
              >
                @if (link.icon) {
                  <img [src]="link.icon" alt="" width="18" height="18" loading="lazy" />
                }
                <span>{{ link.site }}</span>
                @if (link.language) {
                  <span class="lang">{{ link.language }}</span>
                }
                @if (link.mine) {
                  <span class="tag">
                    <svg viewBox="0 0 24 24" width="12" height="12" aria-hidden="true">
                      <path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                    Your service
                  </span>
                }
              </a>
            </li>
          }
          @for (c of customs(); track c.name) {
            <li>
              <a
                class="watch-btn own"
                [href]="c.url"
                target="_blank"
                rel="noopener noreferrer nofollow"
                [attr.aria-label]="'Watch on ' + c.name + ', your own platform, opens in a new tab'"
              >
                <svg class="own-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                  <path d="M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
                </svg>
                <span>{{ c.name }}</span>
                <span class="tag">Yours</span>
              </a>
            </li>
          }
        </ul>
      </div>
    }
  `,
  styles: `
    .head { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: 4px 12px; margin: 0 0 0.5rem; }
    h4 { margin: 0; }
    .edit { display: inline-flex; align-items: center; gap: 5px; font-size: 0.8rem; color: var(--text-muted); text-decoration: none; }
    .edit:hover { color: var(--accent); }
    .edit:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; border-radius: 4px; }
    ul {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .watch-btn {
      --site-color: var(--accent);
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      min-height: 2.5rem;
      padding: 0 0.9rem 0 0.75rem;
      border-radius: 8px;
      border: 1px solid var(--border-subtle);
      border-left: 4px solid var(--site-color);
      background-color: var(--bg-lighter);
      color: var(--text-strong);
      font-weight: 600;
      font-size: 0.9rem;
      text-decoration: none;
      transition: background-color 150ms ease;
    }
    .watch-btn:hover { background-color: var(--surface-hover); }
    .watch-btn:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
    img {
      width: 18px;
      height: 18px;
      padding: 2px;
      border-radius: 4px;
      background-color: var(--site-color);
      object-fit: contain;
    }
    .watch-btn.mine {
      border-color: var(--site-color);
      background-color: color-mix(in srgb, var(--site-color) 12%, var(--bg-lighter));
    }
    .watch-btn.own { --site-color: var(--accent); border-style: dashed; border-left-style: solid; }
    .own-icon { color: var(--accent); flex: none; }
    .tag {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      padding: 1px 7px;
      border-radius: 999px;
      background-color: var(--accent);
      color: var(--on-accent);
      font-size: 0.7rem;
      font-weight: 700;
    }
    .lang {
      font-size: 0.75rem;
      font-weight: 500;
      color: var(--text-muted);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WhereToWatchComponent {
  protected store = inject(MyServicesService);

  links = input<StreamingLink[] | undefined>();
  /** The show's title, for custom platforms whose link searches with {title}. */
  title = input<string | undefined>();

  /** The viewer's services first (keeping AniList's order within each group). */
  protected sorted = computed<WatchLink[]>(() => {
    const links = (this.links() ?? []).map((l) => ({ ...l, mine: this.store.isMine(l.site) }));
    return [...links.filter((l) => l.mine), ...links.filter((l) => !l.mine)];
  });

  protected customs = computed(() =>
    this.store.selection().customPlatforms.map((c) => ({ name: c.name, url: this.store.customUrl(c, this.title()) }))
  );
}
