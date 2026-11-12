import { Component, ChangeDetectionStrategy, computed, inject } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { AiringSoonComponent } from "./weekly-schedule/airing-soon/airing-soon.component";
import { WeeklyScheduleComponent } from "./weekly-schedule/weekly-schedule.component";
import { MediaSkeletonComponent } from './shared-home/media-skeleton/media-skeleton.component';
import { SidebarComponent } from "../shared-components/sidebar/sidebar.component";
import { SeasonPopComponent } from "./weekly-schedule/season-pop/season-pop.component";
import { HeroCarouselComponent } from "./hero-carousel/hero-carousel.component";
import { ContinueWatchingComponent } from '../shared-components/continue-watching/continue-watching.component';
import { AuthService } from '../auth/auth.service';
import { LiveBannerComponent } from "./live-banner/live-banner.component";
import { MostHypedComponent } from "./most-hyped/most-hyped.component";

export type ScheduleView = 'mine' | 'all';
const VIEW_STORAGE_KEY = 'alc.scheduleView';

@Component({
  selector: 'app-home',
  imports: [AiringSoonComponent, WeeklyScheduleComponent, MediaSkeletonComponent, SidebarComponent, SeasonPopComponent,
    HeroCarouselComponent, ContinueWatchingComponent, NgTemplateOutlet, LiveBannerComponent, MostHypedComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent {
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  protected signedIn = this.auth.isAuthenticated;
  // Wait for the session restore so signed-in visitors don't see "All shows" flash first
  protected sessionChecked = toSignal(this.auth.whenSessionChecked().pipe(map(() => true)), { initialValue: false });
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
    // Storage unavailable (private mode, blocked site data); the URL still carries the choice
  }
}
