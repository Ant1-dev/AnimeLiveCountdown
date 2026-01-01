import { Component, ChangeDetectionStrategy } from '@angular/core';
import { AiringSoonComponent } from "./weekly-schedule/airing-soon/airing-soon.component";
import { WeeklyScheduleComponent } from "./weekly-schedule/weekly-schedule.component";
import { MediaSkeletonComponent } from './shared-home/media-skeleton/media-skeleton.component';
import { SidebarComponent } from "../shared-components/sidebar/sidebar.component";
import { SeasonPopComponent } from "./weekly-schedule/season-pop/season-pop.component";
import { HeroCarouselComponent } from "./hero-carousel/hero-carousel.component";

@Component({
  selector: 'app-home',
  imports: [AiringSoonComponent, WeeklyScheduleComponent, MediaSkeletonComponent, SidebarComponent, SeasonPopComponent, HeroCarouselComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent {

}
