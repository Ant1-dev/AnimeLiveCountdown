import { Component, input } from '@angular/core';
import { Media } from '../../../schedule.model';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-result',
  imports: [CommonModule],
  templateUrl: './result.component.html',
  styleUrl: './result.component.css'
})
export class ResultComponent {
  result = input<Media>();


}
