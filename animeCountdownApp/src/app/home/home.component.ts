import { Component, ChangeDetectionStrategy } from '@angular/core';
import { TrendingComponent } from "./weekly-schedule/trending/trending.component";
import { AiringSoonComponent } from "./weekly-schedule/airing-soon/airing-soon.component";
import { WeeklyScheduleComponent } from "./weekly-schedule/weekly-schedule.component";
import { MediaSkeletonComponent } from './shared-home/media-skeleton/media-skeleton.component';

@Component({
  selector: 'app-home',
  imports: [TrendingComponent, AiringSoonComponent, WeeklyScheduleComponent, MediaSkeletonComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent {

}
