import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map, tap } from 'rxjs';
import { ShowsComponent } from '../home/weekly-schedule/shows/shows.component';
import { SeoService, breadcrumbs } from '../seo/seo.service';
import { injectResponseStatus } from '../seo/http-status';
import { WEEK_DAYS, WeekDay, dayLabel } from '../shared/week-days';


/** /schedule/monday etc.: every episode airing that weekday, a landing page for "monday anime" searches. */
@Component({
  selector: 'app-schedule-day',
  imports: [ShowsComponent, RouterLink],
  template: `
    @if (day(); as d) {
    <section class="page">
      <h1 class="section-title">{{ label(d) }} Anime Schedule</h1>
      <p class="lede">Every anime episode airing on {{ label(d) }} this week, with live countdowns shown in your time zone.</p>
      <nav class="days" aria-label="Schedule by day">
        @for (other of days; track other) {
        <a [routerLink]="['/schedule', other]" [class.active]="other === d" [attr.aria-current]="other === d ? 'page' : null">{{ label(other) }}</a>
        }
      </nav>
      <!-- Recreated per day: app-shows loads its list once on init -->
      @for (current of [d]; track current) {
      <app-shows [weekDay]="current" renderPriority="high" />
      }
    </section>
    }
  `,
  styles: `
    .page { padding: 16px 0; }
    .lede { color: var(--text-secondary); max-width: 720px; }
    .days { display: flex; flex-wrap: wrap; gap: 8px; margin: 12px 0 20px; }
    .days a { padding: 6px 12px; border-radius: 999px; border: 1px solid var(--border-subtle); color: var(--text-primary); text-decoration: none; font-size: 0.9rem; }
    .days a:hover { border-color: var(--accent); }
    .days a.active { background: var(--accent); border-color: var(--accent); color: var(--on-accent); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleDayComponent {
  private seo = inject(SeoService);
  private setStatus = injectResponseStatus();
  protected readonly days = WEEK_DAYS;
  protected readonly label = dayLabel;

  protected day = toSignal(
    inject(ActivatedRoute).paramMap.pipe(
      map((params) => (params.get('day') ?? '').toLowerCase()),
      map((d) => (WEEK_DAYS as readonly string[]).includes(d) ? (d as WeekDay) : null),
      tap((d) => this.applySeo(d)),
    ),
    { initialValue: null },
  );


  private applySeo(day: WeekDay | null): void {
    if (!day) {
      this.setStatus(404);
      this.seo.setPage({ title: 'Page not found', description: 'There is no schedule for that day.', path: '/schedule', noindex: true });
      return;
    }
    const name = dayLabel(day);
    this.seo.setPage({
      title: `${name} Anime Schedule: Episodes Airing ${name}`,
      description: `Every anime episode airing on ${name} this week, with live countdowns in your time zone, release times and where to watch.`,
      path: `/schedule/${day}`,
      jsonLd: [breadcrumbs([['Home', '/'], [`${name} anime schedule`, `/schedule/${day}`]])],
    });
  }
}
