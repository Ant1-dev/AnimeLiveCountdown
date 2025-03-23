import { Component } from '@angular/core';
import { SearchbarComponent } from "./searchbar/searchbar.component";
import { RouterLink } from '@angular/router';

@Component({
    selector: 'app-header',
    imports: [SearchbarComponent, RouterLink],
    templateUrl: './header.component.html',
    styleUrl: './header.component.css'
})
export class HeaderComponent {

}
