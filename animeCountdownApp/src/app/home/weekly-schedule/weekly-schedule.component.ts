import { Component } from '@angular/core';
import { ShowsComponent } from "./shows/shows.component";
import { CollapsibleComponent } from '../collapsible/collapsible.component';

@Component({
    selector: 'app-weekly-schedule',
    imports: [ShowsComponent, CollapsibleComponent],
    templateUrl: './weekly-schedule.component.html',
    styleUrl: './weekly-schedule.component.css'
})
export class WeeklyScheduleComponent {
    weekDays: string[] = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
}
