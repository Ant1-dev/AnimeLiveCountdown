import { Component } from '@angular/core';
import { TrendingComponent } from "./weekly-schedule/trending/trending.component";
import { AiringSoonComponent } from "./weekly-schedule/airing-soon/airing-soon.component";
import { WeeklyScheduleComponent } from "./weekly-schedule/weekly-schedule.component";

@Component({
  selector: 'app-home',
  imports: [TrendingComponent, AiringSoonComponent, WeeklyScheduleComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {

}
