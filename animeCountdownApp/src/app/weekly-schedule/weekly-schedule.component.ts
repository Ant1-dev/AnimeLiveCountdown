import { Component } from '@angular/core';
import { ShowsComponent } from "./shows/shows.component";

@Component({
    selector: 'app-weekly-schedule',
    imports: [ShowsComponent],
    templateUrl: './weekly-schedule.component.html',
    styleUrl: './weekly-schedule.component.css'
})
export class WeeklyScheduleComponent {
    weekDays: string[] = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
}
