import { Component } from '@angular/core';
import { ShowsComponent } from "./shows/shows.component";

@Component({
  selector: 'app-weekly-schedule',
  standalone: true,
  imports: [ShowsComponent],
  templateUrl: './weekly-schedule.component.html',
  styleUrl: './weekly-schedule.component.css'
})
export class WeeklyScheduleComponent {

}
