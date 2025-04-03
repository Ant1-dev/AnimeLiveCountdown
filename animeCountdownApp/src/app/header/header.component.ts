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
export class HeaderComponent implements OnInit {
    language!: FormGroup;

    stateOptions: any[] = [
        { label: 'Sub', value: 'Sub'},
        { label: 'Dub', value: 'Dub'}
    ];

    ngOnInit(): void {
        this.language = new FormGroup({
            value: new FormControl('Sub')
        });
    }

}
