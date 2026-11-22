import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MyServicesService } from '../../services/my-services.service';

/**
 * "My services only" switch for schedule and following views. Remembered on this device. With no
 * services picked it links to the My services page instead.
 */
@Component({
  selector: 'app-my-services-toggle',
  imports: [RouterLink],
  template: `
    @if (store.loaded()) {
      @if (store.hasServices()) {
        <span class="wrap">
          <button
            type="button"
            class="toggle"
            role="switch"
            [attr.aria-checked]="store.onlyMine()"
            (click)="store.setOnlyMine(!store.onlyMine())"
          >
            <svg class="tv" viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
              <rect x="3" y="5" width="18" height="12" rx="2" fill="none" stroke="currentColor" stroke-width="1.8" />
              <path d="M8 20h8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            </svg>
            <span>My services only</span>
            <span class="track" aria-hidden="true"><span class="thumb"></span></span>
          </button>
          <a class="edit" routerLink="/my-services" aria-label="Edit my services" title="Edit my services">
            <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
              <path d="M4 20h4L19 9l-4-4L4 16v4zM13.5 6.5l4 4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
            </svg>
          </a>
        </span>
      } @else {
        <a class="pick" routerLink="/my-services">
          <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
            <rect x="3" y="5" width="18" height="12" rx="2" fill="none" stroke="currentColor" stroke-width="1.8" />
            <path d="M8 20h8M12 8.5v5M9.5 11h5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
          </svg>
          Pick your streaming services
        </a>
      }
    }
  `,
  styles: `
    :host { display: inline-flex; }
    .wrap { display: inline-flex; align-items: center; gap: 4px; }
    .toggle, .pick, .edit {
      display: inline-flex; align-items: center; gap: 8px;
      min-height: 36px; padding: 0 12px;
      border: 1px solid var(--border-strong); border-radius: 999px;
      background: none; color: var(--text-primary);
      font: inherit; font-size: 14px; text-decoration: none; cursor: pointer;
    }
    .edit { padding: 0; width: 36px; justify-content: center; color: var(--text-secondary); }
    .toggle:hover, .pick:hover, .edit:hover { background: var(--surface-hover); }
    .toggle:focus-visible, .pick:focus-visible, .edit:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
    .toggle[aria-checked='true'] { border-color: var(--accent); }
    .tv { color: var(--accent); }
    .track { position: relative; width: 30px; height: 18px; border-radius: 999px; background: var(--border-strong); transition: background-color 150ms ease; }
    .thumb { position: absolute; top: 2px; left: 2px; width: 14px; height: 14px; border-radius: 50%; background: var(--bg-card, #fff); transition: transform 150ms ease; }
    .toggle[aria-checked='true'] .track { background: var(--accent); }
    .toggle[aria-checked='true'] .thumb { transform: translateX(12px); }
    @media (prefers-reduced-motion: reduce) { .track, .thumb { transition: none; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyServicesToggleComponent {
  protected store = inject(MyServicesService);
}
