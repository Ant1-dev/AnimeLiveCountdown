import { Component, ChangeDetectionStrategy } from '@angular/core';
import { Skeleton } from 'primeng/skeleton';

@Component({
  selector: 'app-media-skeleton',
  imports: [Skeleton],
  templateUrl: './media-skeleton.component.html',
  styleUrl: './media-skeleton.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MediaSkeletonComponent {

}
