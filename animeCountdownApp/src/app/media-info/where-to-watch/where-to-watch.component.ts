import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { StreamingLink } from '../../models/media.info.model';

/** Streaming buttons from AniList's externalLinks (type STREAMING). Renders nothing when empty. */
@Component({
  selector: 'app-where-to-watch',
  template: `
    @if (links()?.length) {
      <div class="watch">
        <h4>Where to watch</h4>
        <ul>
          @for (link of links(); track link.url) {
            <li>
              <a
                class="watch-btn"
                [href]="link.url"
                target="_blank"
                rel="noopener noreferrer"
                [style.--site-color]="link.color || null"
                [attr.aria-label]="'Watch on ' + link.site + (link.language ? ' (' + link.language + ')' : '') + ', opens in a new tab'"
              >
                @if (link.icon) {
                  <img [src]="link.icon" alt="" width="18" height="18" loading="lazy" />
                }
                <span>{{ link.site }}</span>
                @if (link.language) {
                  <span class="lang">{{ link.language }}</span>
                }
              </a>
            </li>
          }
        </ul>
      </div>
    }
  `,
  styles: `
    h4 { margin: 0 0 0.5rem; }
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
    .lang {
      font-size: 0.75rem;
      font-weight: 500;
      color: var(--text-muted);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WhereToWatchComponent {
  links = input<StreamingLink[] | undefined>();
}
