import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SeoService, breadcrumbs } from '../seo/seo.service';
import { CONTACT_EMAIL, LEGAL_UPDATED } from './legal';

@Component({
  selector: 'app-privacy-policy',
  imports: [RouterLink],
  templateUrl: './privacy-policy.component.html',
  styleUrl: './legal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivacyPolicyComponent {
  protected readonly email = CONTACT_EMAIL;
  protected readonly updated = LEGAL_UPDATED;

  constructor() {
    inject(SeoService).setPage({
      title: 'Privacy Policy',
      description: 'What Anime Live Countdown collects when you use the site or sign in, how it is used, and how to have it deleted.',
      path: '/privacy',
      jsonLd: [breadcrumbs([['Home', '/'], ['Privacy Policy', '/privacy']])],
    });
  }
}
