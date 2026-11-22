import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { MyServicesService, isValidPlatformUrl } from '../services/my-services.service';
import { SeoService } from '../seo/seo.service';
import { CustomPlatform, MyServicesSelection } from '../models/streaming-service.model';
import { MAX_CUSTOM_PLATFORMS, MAX_PLATFORM_NAME_LENGTH, MAX_PLATFORM_URL_LENGTH } from '../shared/my-services-rules';
import { MyServicesToggleComponent } from '../shared-components/my-services-toggle/my-services-toggle.component';

/** Settings page: pick the streaming services you use and add your own platforms. Works signed out. */
@Component({
  selector: 'app-my-services',
  imports: [FormsModule, RouterLink, MyServicesToggleComponent],
  template: `
    <div class="section-header">
      <h1 class="section-title">My services</h1>
      <div class="gradient-bar"></div>
    </div>
    <p class="intro">
      Pick where you watch. Your services are listed first on every show, and the schedule can hide
      shows that aren't on them.
      @if (signedIn()) {
        Saved to your account.
      } @else {
        Saved in this browser; sign in to keep them on your account.
      }
    </p>

    <div class="layout">
      <section class="card" aria-labelledby="services-heading">
        <div class="card-head">
          <h2 id="services-heading">Streaming services</h2>
          <span class="status" role="status">{{ status() }}</span>
        </div>
        @if (!store.loaded() || !store.catalog().length) {
          <p class="muted">Loading…</p>
        } @else {
          <ul class="chips">
            @for (s of store.catalog(); track s.key) {
              <li>
                <button
                  type="button"
                  class="chip"
                  [class.on]="store.selected().has(s.key)"
                  [attr.aria-pressed]="store.selected().has(s.key)"
                  [style.--site-color]="s.color"
                  [disabled]="busy()"
                  (click)="toggle(s.key)"
                >
                  <span class="dot" aria-hidden="true"></span>
                  <span>{{ s.label }}</span>
                  @if (store.selected().has(s.key)) {
                    <svg class="check" viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
                      <path d="M5 12.5l4.5 4.5L19 7.5" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                  }
                </button>
              </li>
            }
          </ul>
          @if (store.hasServices()) {
            <div class="filter-row">
              <app-my-services-toggle />
              <span class="muted small">Applies to the weekly schedule, My Schedule and Following.</span>
            </div>
          }
        }
      </section>

      <section class="card" aria-labelledby="custom-heading">
        <h2 id="custom-heading">Your own platforms</h2>
        <p>
          Watch somewhere that isn't listed? Add it with a link. Put <code>{{ placeholder }}</code> in the link
          to search for the show, for example <code>https://example.com/search?q={{ placeholder }}</code>.
        </p>

        @if (customs().length) {
          <ul class="customs">
            @for (c of customs(); track c.name) {
              <li>
                <svg class="own-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                  <path d="M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" />
                </svg>
                <div class="custom-main">
                  <span class="custom-name">{{ c.name }}</span>
                  <span class="custom-url">{{ c.url }}</span>
                </div>
                <button type="button" class="icon-btn" [disabled]="busy()" (click)="remove(c)" [attr.aria-label]="'Remove ' + c.name" [title]="'Remove ' + c.name">
                  <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                    <path d="M4 7h16M9 7V4.5h6V7M6.5 7l1 12.5h9l1-12.5M10 11v5M14 11v5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </button>
              </li>
            }
          </ul>
        }

        @if (customs().length < maxCustom) {
          <form class="add-form" (ngSubmit)="add()" novalidate>
            <div class="field">
              <label for="platform-name" class="label">Name</label>
              <input id="platform-name" name="name" type="text" autocomplete="off" [maxlength]="maxName"
                     placeholder="Plex" [ngModel]="name()" (ngModelChange)="name.set($event); formError.set('')" />
            </div>
            <div class="field grow">
              <label for="platform-url" class="label">Link</label>
              <input id="platform-url" name="url" type="url" autocomplete="off" spellcheck="false" [maxlength]="maxUrl"
                     placeholder="https://…" [ngModel]="url()" (ngModelChange)="url.set($event); formError.set('')"
                     (blur)="checkUrl()" [attr.aria-invalid]="urlInvalid()" aria-describedby="platform-error" />
            </div>
            <button type="submit" class="btn primary" [disabled]="busy()">
              <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
                <path d="M12 5v14M5 12h14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" />
              </svg>
              Add platform
            </button>
          </form>
        } @else {
          <p class="muted small">You've added the most platforms you can ({{ maxCustom }}). Remove one to add another.</p>
        }
        <p id="platform-error" class="error" role="alert">{{ formError() }}</p>
      </section>
    </div>

    <p class="muted small back"><a routerLink="/">Back to the schedule</a></p>
  `,
  styles: `
    :host { display: block; max-width: 1100px; margin: 0 auto 3rem; }
    .section-header { margin: 2rem 0 1rem; }
    .section-title { font-size: 1.5rem; font-weight: 700; color: var(--accent); margin: 0; }
    .gradient-bar { height: 3px; background: linear-gradient(to right, var(--accent), transparent); margin-top: 0.5rem; }
    .intro { margin: 0 0 1.25rem; color: var(--text-secondary); line-height: 1.5; max-width: 70ch; }
    .layout { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 420px), 1fr)); gap: 16px; }
    .card { background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: 12px; padding: 20px; min-width: 0; }
    .card h2 { margin: 0 0 8px; font-size: 1.1rem; color: var(--text-primary); }
    .card p { margin: 0 0 12px; line-height: 1.5; color: var(--text-secondary); font-size: 14px; }
    .card-head { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
    .status { font-size: 13px; color: var(--text-muted); min-height: 1em; }
    .muted { color: var(--text-muted) !important; }
    .small { font-size: 13px !important; }
    .error { color: var(--danger) !important; min-height: 1.2em; margin: 8px 0 0 !important; font-size: 13px !important; }
    code { font-size: 12px; color: var(--text-primary); word-break: break-all; }

    .chips { display: flex; flex-wrap: wrap; gap: 8px; list-style: none; margin: 4px 0 16px; padding: 0; }
    .chip {
      --site-color: var(--accent);
      display: inline-flex; align-items: center; gap: 8px;
      min-height: 40px; padding: 0 14px 0 12px;
      border-radius: 999px; border: 1px solid var(--border-strong);
      background: none; color: var(--text-primary); font: inherit; font-size: 14px; cursor: pointer;
      transition: background-color 150ms ease, border-color 150ms ease;
    }
    .chip:hover:not(:disabled) { background: var(--surface-hover); }
    .chip.on { border-color: var(--site-color); background: color-mix(in srgb, var(--site-color) 14%, transparent); font-weight: 600; }
    .chip:disabled { opacity: 0.7; cursor: default; }
    .chip:focus-visible, .btn:focus-visible, .icon-btn:focus-visible, input:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
    .dot { width: 10px; height: 10px; border-radius: 50%; background: var(--site-color); flex: none; }
    .check { color: var(--text-strong); }
    .filter-row { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px; border-top: 1px solid var(--border-subtle); padding-top: 14px; }

    .customs { list-style: none; margin: 0 0 12px; padding: 0; }
    .customs li { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-top: 1px solid var(--border-subtle); }
    .own-icon { flex: none; color: var(--accent); }
    .custom-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
    .custom-name { font-weight: 600; color: var(--text-primary); }
    .custom-url { font-size: 12px; color: var(--text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .icon-btn { flex: none; display: inline-flex; align-items: center; justify-content: center; width: 36px; height: 36px; border-radius: 8px; border: 1px solid transparent; background: none; color: var(--text-secondary); cursor: pointer; }
    .icon-btn:hover:not(:disabled) { color: var(--danger); border-color: var(--border-subtle); }

    .add-form { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 10px; }
    .field { display: flex; flex-direction: column; gap: 4px; flex: 0 1 160px; min-width: 0; }
    .field.grow { flex: 1 1 220px; }
    .label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
    input { padding: 9px 12px; border-radius: 8px; border: 1px solid var(--border-strong); background: var(--bg-dark); color: var(--text-primary); font: inherit; font-size: 14px; min-width: 0; }
    input[aria-invalid='true'] { border-color: var(--danger); }
    .btn { display: inline-flex; align-items: center; gap: 6px; border: 1px solid var(--border-strong); border-radius: 8px; padding: 9px 14px; background: none; color: var(--text-primary); font: inherit; font-size: 14px; cursor: pointer; white-space: nowrap; }
    .btn.primary { background: var(--accent); border-color: var(--accent); color: var(--on-accent); font-weight: 600; }
    .btn:disabled { opacity: 0.6; cursor: default; }
    .back { margin-top: 1.5rem; }
    .back a { color: inherit; }
    @media screen and (max-width: 600px) {
      :host { padding: 0 16px; }
      .field, .field.grow { flex-basis: 100%; }
      .btn { width: 100%; justify-content: center; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyServicesComponent {
  private readonly _seo = inject(SeoService).setPage({
    title: 'My streaming services',
    description: 'Pick the streaming services you use to see where to watch each show.',
    path: '/my-services',
    noindex: true,
  });

  protected store = inject(MyServicesService);
  protected signedIn = inject(AuthService).isAuthenticated;

  protected readonly placeholder = '{title}';
  protected readonly maxCustom = MAX_CUSTOM_PLATFORMS;
  protected readonly maxName = MAX_PLATFORM_NAME_LENGTH;
  protected readonly maxUrl = MAX_PLATFORM_URL_LENGTH;

  protected customs = computed(() => this.store.selection().customPlatforms);
  protected busy = signal(false);
  protected status = signal('');
  protected name = signal('');
  protected url = signal('');
  protected formError = signal('');
  protected urlInvalid = signal(false);

  protected toggle(key: string): void {
    const current = this.store.selection();
    const services = current.services.includes(key)
      ? current.services.filter((k) => k !== key)
      : [...current.services, key];
    this.persist({ ...current, services });
  }

  protected checkUrl(): void {
    const value = this.url().trim();
    this.urlInvalid.set(value !== '' && !isValidPlatformUrl(value));
    if (this.urlInvalid()) this.formError.set('Use a full link starting with http:// or https://');
  }

  protected add(): void {
    const platform: CustomPlatform = { name: this.name().trim(), url: this.url().trim() };
    if (!platform.name) {
      this.formError.set('Give the platform a name');
      return;
    }
    if (!isValidPlatformUrl(platform.url)) {
      this.urlInvalid.set(true);
      this.formError.set('Use a full link starting with http:// or https://');
      return;
    }
    const current = this.store.selection();
    this.persist({ ...current, customPlatforms: [...current.customPlatforms, platform] }, () => {
      this.name.set('');
      this.url.set('');
      this.urlInvalid.set(false);
    });
  }

  protected remove(platform: CustomPlatform): void {
    const current = this.store.selection();
    this.persist({ ...current, customPlatforms: current.customPlatforms.filter((c) => c !== platform) });
  }

  private persist(next: MyServicesSelection, onSaved?: () => void): void {
    this.busy.set(true);
    this.status.set('Saving…');
    this.store.save(next).subscribe({
      next: () => {
        this.busy.set(false);
        this.status.set('Saved');
        this.formError.set('');
        onSaved?.();
      },
      error: (err: Error) => {
        this.busy.set(false);
        this.status.set('');
        this.formError.set(err.message);
      },
    });
  }
}
