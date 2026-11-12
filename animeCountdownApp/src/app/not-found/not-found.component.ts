import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { SeoService } from '../seo/seo.service';
import { injectResponseStatus } from '../seo/http-status';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  template: `
    <section class="not-found">
      <h1>Page not found</h1>
      <p>There’s nothing at this address. It may have moved, or the link may be mistyped.</p>
      <ul>
        <li><a routerLink="/">This week’s anime schedule</a></li>
        <li><a routerLink="/seasons">This season’s anime</a></li>
        <li><a routerLink="/most-hyped">Most hyped this week</a></li>
      </ul>
    </section>
  `,
  styles: `
    .not-found { max-width: 640px; margin: 64px auto; padding: 0 16px; text-align: center; color: var(--text-secondary); }
    h1 { color: var(--text-strong); margin-bottom: 12px; }
    ul { list-style: none; padding: 0; display: flex; flex-direction: column; gap: 8px; margin-top: 20px; }
    a { color: var(--accent); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotFoundComponent {
  constructor() {
    injectResponseStatus()(404);
    inject(SeoService).setPage({
      title: 'Page not found',
      description: 'This page doesn’t exist. Browse this week’s anime schedule or this season’s shows instead.',
      path: inject(Router).url.split(/[?#]/)[0],
      noindex: true,
    });
  }
}
