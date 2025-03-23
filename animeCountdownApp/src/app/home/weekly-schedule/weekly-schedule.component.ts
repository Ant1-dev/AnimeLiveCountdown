import { Component } from '@angular/core';
import { ShowsComponent } from "./shows/shows.component";
import { CollapsibleComponent } from '../collapsible/collapsible.component';
import { SkeletonModule } from 'primeng/skeleton';

@Component({
  selector: 'app-weekly-schedule',
  standalone: true,
  imports: [
    ShowsComponent, 
    CollapsibleComponent,
    SkeletonModule
  ],
  templateUrl: './weekly-schedule.component.html',
  styleUrl: './weekly-schedule.component.css'
})
export class WeeklyScheduleComponent {
  weekDays: string[] = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
}