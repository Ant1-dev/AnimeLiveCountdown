import { CommonModule } from '@angular/common';
import { Component, input, OnInit } from '@angular/core';
import { Media } from '../../../schedule.model';

@Component({
  selector: 'app-show',
  imports: [CommonModule],
  templateUrl: './show.component.html',
  styleUrl: './show.component.css',
})
export class ShowComponent implements OnInit {
  media = input<Media | undefined>(undefined);
  ngOnInit(): void {
    console.log('show is:', this.media());
  }
}
