import { Pipe, PipeTransform } from '@angular/core';
import { optimizedImageUrl } from './images';

/** For plain <img [src]> and backgrounds: `[src]="url | optimizedImage: 460"` (width in pixels). */
@Pipe({ name: 'optimizedImage' })
export class OptimizedImagePipe implements PipeTransform {
  transform(src: string | null | undefined, width: number): string {
    return optimizedImageUrl(src, width);
  }
}
