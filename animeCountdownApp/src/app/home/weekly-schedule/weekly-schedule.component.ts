import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { ShowsComponent } from "./shows/shows.component";
import { CollapsibleComponent } from '../shared-home/collapsible/collapsible.component';
import { SkeletonModule } from 'primeng/skeleton';
import { MediaSkeletonComponent } from "../shared-home/media-skeleton/media-skeleton.component";

@Component({
  selector: 'app-weekly-schedule',
  imports: [
    ShowsComponent,
    CollapsibleComponent,
    SkeletonModule,
    MediaSkeletonComponent,
],
  templateUrl: './weekly-schedule.component.html',
  styleUrl: './weekly-schedule.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeeklyScheduleComponent {
  // 'mine' = My Schedule (followed shows only)
  mode = input<'all' | 'mine'>('all');
  weekDays: string[] = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
}