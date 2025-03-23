import { Component, inject, input } from '@angular/core';
import { Media } from '../../../models/schedule.model';
import { CommonModule } from '@angular/common';
import { MediaInfoService } from '../../../services/media.info.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-result',
  imports: [CommonModule, RouterLink],
  templateUrl: './result.component.html',
  styleUrl: './result.component.css',
})
export class ResultComponent {
  result = input<Media>();
  mediaInfoServe = inject(MediaInfoService);

  retrieveId() {
    this.mediaInfoServe.getMediaId(this.result()!.id);
  }
}
