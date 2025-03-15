import { Component } from '@angular/core';
import { SearchbarComponent } from "./searchbar/searchbar.component";

@Component({
    selector: 'app-header',
    imports: [SearchbarComponent],
    templateUrl: './header.component.html',
    styleUrl: './header.component.css'
})
export class HeaderComponent {

}
