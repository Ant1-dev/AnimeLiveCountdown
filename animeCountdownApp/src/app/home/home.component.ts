import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SidebarComponent } from '../shared-components/sidebar/sidebar.component';
import { HeroCarouselComponent } from './hero-carousel/hero-carousel.component';
import { LiveBannerComponent } from './live-banner/live-banner.component';
import { HomeFeedComponent } from './home-feed/home-feed.component';
import { SeoService } from '../seo/seo.service';
import { WEEK_DAYS, dayLabel } from '../shared/week-days';

@Component({
  selector: 'app-home',
  imports: [SidebarComponent, HeroCarouselComponent, LiveBannerComponent, HomeFeedComponent, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent {
  protected readonly days = WEEK_DAYS;

  constructor() {
    inject(SeoService).setPage({
      title: 'Anime Airing Schedule & Episode Countdowns',
      description: 'See exactly when every new anime episode airs, with live countdowns in your time zone, this season’s schedule, and reminders so you never miss a release.',
      path: '/',
    });
  }

  protected label = dayLabel;
}
