import { Component } from '@angular/core';
import { ShowComponent } from "./show/show.component";

@Component({
  selector: 'app-shows',
  standalone: true,
  imports: [ShowComponent],
  templateUrl: './shows.component.html',
  styleUrl: './shows.component.css'
})
export class ShowsComponent {

}
