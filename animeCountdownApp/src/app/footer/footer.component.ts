import { Component, ChangeDetectionStrategy } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { WEEK_DAYS } from '../shared/week-days';


@Component({
    selector: 'app-footer',
    imports: [MatIconModule, RouterLink],
    templateUrl: './footer.component.html',
    styleUrl: './footer.component.css',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FooterComponent {
  protected readonly days = WEEK_DAYS;

}
