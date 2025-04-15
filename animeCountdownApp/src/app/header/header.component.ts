import { Component, OnInit } from '@angular/core';
import { SearchbarComponent } from "./searchbar/searchbar.component";
import { RouterLink } from '@angular/router';
import { SelectButtonModule } from 'primeng/selectbutton';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatIcon } from '@angular/material/icon';

@Component({
    selector: 'app-header',
    imports: [SearchbarComponent, RouterLink, SelectButtonModule, ReactiveFormsModule, MatIcon],
    templateUrl: './header.component.html',
    styleUrl: './header.component.css'
})
export class HeaderComponent {
}
