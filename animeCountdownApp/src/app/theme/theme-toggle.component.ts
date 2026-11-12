import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { ThemeService } from './theme.service';

@Component({
  selector: 'app-theme-toggle',
  imports: [MatIcon],
  template: `
    <button
      type="button"
      class="theme-toggle"
      (click)="theme.toggle()"
      [attr.aria-label]="label()"
      [attr.title]="label()"
    >
      <mat-icon [fontIcon]="theme.theme() === 'dark' ? 'light_mode' : 'dark_mode'"></mat-icon>
    </button>
  `,
  styles: `
    :host { display: inline-flex; }
    .theme-toggle {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 2.5rem;
      height: 2.5rem;
      border: none;
      border-radius: 50%;
      background: none;
      color: var(--text-strong);
      cursor: pointer;
      transition: background-color 150ms ease;
    }
    .theme-toggle:hover { background-color: var(--surface-hover); }
    .theme-toggle:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
    mat-icon { font-size: 26px; width: 26px; height: 26px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThemeToggleComponent {
  protected theme = inject(ThemeService);
  protected label = computed(() =>
    this.theme.theme() === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'
  );
}
