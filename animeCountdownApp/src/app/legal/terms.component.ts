import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SeoService, breadcrumbs } from '../seo/seo.service';
import { CONTACT_EMAIL, LEGAL_UPDATED } from './legal';

@Component({
  selector: 'app-terms',
  imports: [RouterLink],
  templateUrl: './terms.component.html',
  styleUrl: './legal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TermsComponent {
  protected readonly email = CONTACT_EMAIL;
  protected readonly updated = LEGAL_UPDATED;

  constructor() {
    inject(SeoService).setPage({
      title: 'Terms of Service',
      description: 'The terms for using Anime Live Countdown, a free, fan-made anime airing schedule and countdown site.',
      path: '/terms',
      jsonLd: [breadcrumbs([['Home', '/'], ['Terms of Service', '/terms']])],
    });
  }
}
