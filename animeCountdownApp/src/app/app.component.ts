import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from "./header/header.component";
import { TrendingComponent } from "./trending/trending.component";
import { MondayComponent } from "./monday/monday.component";
import { TuesdayComponent } from "./tuesday/tuesday.component";
import { WednesdayComponent } from "./wednesday/wednesday.component";
import { ThursdayComponent } from "./thursday/thursday.component";
import { FridayComponent } from "./friday/friday.component";
import { SaturndayComponent } from "./saturnday/saturnday.component";
import { SundayComponent } from "./sunday/sunday.component";
import { FooterComponent } from "./footer/footer.component";


@Component({
  selector: 'app-root',
  standalone: true,
  imports: [HeaderComponent, TrendingComponent, MondayComponent, TuesdayComponent, WednesdayComponent, ThursdayComponent, FridayComponent, SaturndayComponent, SundayComponent, FooterComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'animeCountdownApp';
}
