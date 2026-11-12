import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { ThemeToggleComponent } from './theme-toggle.component';

/** Season browser link + theme toggle, kept in one component so the shared header stays small. */
@Component({
  selector: 'app-header-extras',
  imports: [RouterLink, RouterLinkActive, MatIcon, ThemeToggleComponent],
  template: `
    <a
      class="seasons-link"
      routerLink="/seasons"
      routerLinkActive="active"
      aria-label="Browse anime by season"
      title="Browse seasons"
    >
      <mat-icon fontIcon="calendar_month"></mat-icon>
      <span class="label">Seasons</span>
    </a>
    <app-theme-toggle />
  `,
  styles: `
    :host { display: inline-flex; align-items: center; gap: 0.25rem; }
    .seasons-link {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      height: 2.5rem;
      padding: 0 0.6rem;
      border-radius: 999px;
      color: var(--text-strong);
      text-decoration: none;
      font-weight: 500;
      transition: background-color 150ms ease;
    }
    .seasons-link:hover { background-color: var(--surface-hover); }
    .seasons-link.active { color: var(--accent); }
    .seasons-link:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
    mat-icon { font-size: 24px; width: 24px; height: 24px; }
    @media (max-width: 900px) {
      .label { display: none; }
      .seasons-link { width: 2.5rem; padding: 0; justify-content: center; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderExtrasComponent {}
