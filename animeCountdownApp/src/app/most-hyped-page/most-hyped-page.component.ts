import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MostHypedComponent } from '../home/most-hyped/most-hyped.component';
import { SeoService, breadcrumbs } from '../seo/seo.service';

@Component({
  selector: 'app-most-hyped-page',
  imports: [MostHypedComponent, RouterLink],
  template: `
    <section class="page">
      <h1 class="section-title">Most Hyped Anime This Week</h1>
      <p class="lede">The airing shows fans are reacting to and following most right now, updated through the week.</p>
      <app-most-hyped [limit]="30" [showHeading]="false" />
      <p class="more">Looking for something else? See <a routerLink="/">this week’s full schedule</a> or <a routerLink="/seasons">this season’s anime</a>.</p>
    </section>
  `,
  styles: `
    .page { padding: 16px 0; }
    .lede, .more { color: var(--text-secondary); max-width: 720px; }
    .more a { color: var(--accent); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MostHypedPageComponent {
  constructor() {
    inject(SeoService).setPage({
      title: 'Most Hyped Anime This Week',
      description: 'The currently airing anime fans are reacting to and following most this week, with live countdowns to every next episode.',
      path: '/most-hyped',
      jsonLd: [breadcrumbs([['Home', '/'], ['Most hyped this week', '/most-hyped']])],
    });
  }
}
