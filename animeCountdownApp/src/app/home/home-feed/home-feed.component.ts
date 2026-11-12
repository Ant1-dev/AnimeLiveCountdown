import { Component, ChangeDetectionStrategy, computed, inject } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { AiringSoonComponent } from '../weekly-schedule/airing-soon/airing-soon.component';
import { WeeklyScheduleComponent } from '../weekly-schedule/weekly-schedule.component';
import { MediaSkeletonComponent } from '../shared-home/media-skeleton/media-skeleton.component';
import { SeasonPopComponent } from '../weekly-schedule/season-pop/season-pop.component';
import { ContinueWatchingComponent } from '../../shared-components/continue-watching/continue-watching.component';
import { MostHypedComponent } from '../most-hyped/most-hyped.component';
import { AuthService, readSessionHint } from '../../auth/auth.service';

export type ScheduleView = 'mine' | 'all';
const VIEW_STORAGE_KEY = 'alc.scheduleView';

/**
 * The home page body: "All shows" for everyone, or "My Schedule" for signed-in visitors.
 *
 * The server never knows who is signed in, so it always renders "All shows" (what crawlers see).
 * Signed-out visitors hydrate that HTML as-is. For a returning signed-in visitor the browser picks a
 * different @if branch, and Angular replaces the server-rendered branch with it.
 */
@Component({
  selector: 'app-home-feed',
  imports: [AiringSoonComponent, WeeklyScheduleComponent, MediaSkeletonComponent, SeasonPopComponent,
    ContinueWatchingComponent, NgTemplateOutlet, MostHypedComponent],
  templateUrl: './home-feed.component.html',
  styleUrl: './home-feed.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeFeedComponent {
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  protected signedIn = this.auth.isAuthenticated;
  private checked = toSignal(this.auth.whenSessionChecked().pipe(map(() => true)), { initialValue: false });
  // Returning signed-in visitors wait for the session restore (so "All shows" doesn't flash before
  // "My Schedule"); everyone else gets "All shows" straight away, matching the server-rendered page
  // (no skeleton swap, no layout shift).
  private hadSession = readSessionHint();
  protected sessionChecked = computed(() => this.checked() || !this.hadSession);
  private viewParam = toSignal(this.route.queryParamMap.pipe(map((params) => params.get('view'))), {
    initialValue: null,
  });

  /** Signed in: My Schedule unless the URL or a saved choice says All shows. Signed out: All shows. */
  protected view = computed<ScheduleView>(() => {
    if (!this.signedIn()) return 'all';
    const param = this.viewParam();
    if (param === 'mine' || param === 'all') return param;
    return readSavedView() ?? 'mine';
  });

  setView(view: ScheduleView): void {
    saveView(view);
    this.router.navigate([], { queryParams: { view }, queryParamsHandling: 'merge', replaceUrl: true });
  }
}

function readSavedView(): ScheduleView | null {
  try {
    const saved = localStorage.getItem(VIEW_STORAGE_KEY);
    return saved === 'mine' || saved === 'all' ? saved : null;
  } catch {
    return null;
  }
}

function saveView(view: ScheduleView): void {
  try {
    localStorage.setItem(VIEW_STORAGE_KEY, view);
  } catch {
    // Storage unavailable (private mode, blocked site data, server render); the URL still carries the choice
  }
}
