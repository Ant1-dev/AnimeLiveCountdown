import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from "./header/header.component";
import { TrendingComponent } from "./weekly-schedule/trending/trending.component";
import { FooterComponent } from "./footer/footer.component";
import { AiringSoonComponent } from "./weekly-schedule/airing-soon/airing-soon.component";
import { WeeklyScheduleComponent } from "./weekly-schedule/weekly-schedule.component";


@Component({
    selector: 'app-root',
    imports: [HeaderComponent, TrendingComponent, FooterComponent, AiringSoonComponent, WeeklyScheduleComponent],
    templateUrl: './app.component.html',
    styleUrl: './app.component.css'
})
export class AppComponent {
 
}
